package models.document

import collection.JavaConversions._
import java.io.{ File, InputStream }
import java.nio.file.Files
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import models.{ BaseService, HasDate, Page, SortOrder }
import models.generated.Tables._
import models.generated.tables.records._
import org.joda.time.DateTime
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import storage.{ DB, Uploads }

case class PartOrdering(partId: UUID, seqNo: Int)

@Singleton
class DocumentService @Inject() (uploads: Uploads, implicit val db: DB) extends BaseService with SharingPolicies {
  
  // We use random alphanumeric IDs with 14 chars length (because 62^14 should be enough for anyone (TM))  
  private val ID_LENGTH = 14
  
  // Utility function to check if an ID exists in the DB
  def existsId(id: String) = {
    def checkExists() = db.query { sql =>
      val count = sql.select(DOCUMENT.ID)
         .from(DOCUMENT)
         .where(DOCUMENT.ID.equal(id))
         .fetchArray()
         .length
      
      count > 0
    }
    
    Await.result(checkExists(), 10.seconds)    
  }
  
  def generateRandomID(retriesLeft: Int = 10): String = {
    
    // Takes a set of strings and returns those that already exist in the DB as doc IDs
    def findIds(ids: Set[String])(implicit db: DB) = db.query { sql =>
      sql.select(DOCUMENT.ID)
         .from(DOCUMENT)
         .where(DOCUMENT.ID.in(ids))
         .fetchArray()
         .map(_.value1).toSet    
    }
    
    // Generate 10 random IDs
    val randomIds = 
      (1 to 10).map(_ => RandomStringUtils.randomAlphanumeric(ID_LENGTH).toLowerCase).toSet

    // Match them all against the database and remove those that already exist
    val idsAlreadyInDB = Await.result(findIds(randomIds), 10.seconds)    
    val uniqueIds = randomIds.filter(id => !idsAlreadyInDB.contains(id))
    
    if (uniqueIds.size > 0) {
      uniqueIds.head
    } else if (retriesLeft > 0) {
      Logger.warn("Failed to generate unique random document ID")
      generateRandomID(retriesLeft - 1)
    } else {
      throw new RuntimeException("Failed to create unique document ID")
    }
  }
  
  private def determineAccessLevel(document: DocumentRecord, sharingPolicies: Seq[SharingPolicyRecord], forUser: Option[String]): DocumentAccessLevel =
    forUser match {      
      case Some(user) if (document.getOwner == user) =>
        DocumentAccessLevel.OWNER
      
      case Some(user) =>
        sharingPolicies.filter(_.getSharedWith == user).headOption.flatMap(p => DocumentAccessLevel.withName(p.getAccessLevel)) match {
          // There's a sharing policy for this user
          case Some(policy) => policy
          
          // No sharing policy, but document might still be public
          case None => if (document.getIsPublic) DocumentAccessLevel.READ else DocumentAccessLevel.FORBIDDEN
        }

      case None =>
        if (document.getIsPublic) DocumentAccessLevel.READ else DocumentAccessLevel.FORBIDDEN
    }
  
  /** Creates a new DocumentRecord from an UploadRecord **/
  private[models] def createDocumentFromUpload(upload: UploadRecord) =
    new DocumentRecord(
          generateRandomID(),
          upload.getOwner,
          upload.getCreatedAt,
          upload.getTitle,
          upload.getAuthor,
          null, // TODO timestamp_numeric
          upload.getDateFreeform,
          upload.getDescription,
          upload.getLanguage,
          upload.getSource,
          upload.getEdition,
          upload.getLicense,
          false)
  
  /** Imports document and filepart records to DB, and filepart content to user dir **/
  def importDocument(document: DocumentRecord, fileparts: Seq[(DocumentFilepartRecord, InputStream)]) = db.withTransaction { sql =>
    // Import DocumentRecord 
    sql.insertInto(DOCUMENT).set(document).execute()
    
    // Import DocumentFilepartRecords 
    val inserts = fileparts.map { case (part, _) => sql.insertInto(DOCUMENT_FILEPART).set(part) }    
    sql.batch(inserts:_*).execute()
    
    // Import files
    fileparts.foreach { case (part, stream) =>
      val destination = new File(uploads.getDocumentDir(document.getOwner, document.getId, true).get, part.getFile).toPath
      Files.copy(stream, destination)
    }
  }
    
  /** Changes the public visibility flag for the given document **/
  def setPublicVisibility(docId: String, enabled: Boolean) = db.withTransaction { sql =>
    sql.update(DOCUMENT).set[java.lang.Boolean](DOCUMENT.IS_PUBLIC, enabled).where(DOCUMENT.ID.equal(docId)).execute()
  }
  
  /** Updates the user-defined metadata fields **/
  def updateMetadata(docId: String, title: String, author: Option[String], dateFreeform: Option[String], 
    description: Option[String], language: Option[String], source: Option[String], edition: Option[String],
    license: Option[String]): Future[Boolean] = db.withTransaction { sql =>  

    // If the update sets the document to a non-open license, make sure is_public is set to false
    val hasNonOpenLicense = license.map(acronym =>
      License.fromAcronym(acronym).map(!_.isOpen).getOrElse(true)).getOrElse(true)
      
    val q = sql.update(DOCUMENT)
      .set(DOCUMENT.TITLE, title)
      .set(DOCUMENT.AUTHOR, optString(author))
      .set(DOCUMENT.DATE_FREEFORM, optString(dateFreeform))
      .set(DOCUMENT.DESCRIPTION, optString(description))
      .set(DOCUMENT.LANGUAGE, optString(language))
      .set(DOCUMENT.SOURCE, optString(source))
      .set(DOCUMENT.EDITION, optString(edition))
      .set(DOCUMENT.LICENSE, optString(license))
      
    val rowsAffected =
      if (hasNonOpenLicense)
        q.set[java.lang.Boolean](DOCUMENT.IS_PUBLIC, false)
         .where(DOCUMENT.ID.equal(docId)).execute()
      else
        q.where(DOCUMENT.ID.equal(docId)).execute()
    
    rowsAffected == 1
  }
  
  def updateFilepartMetadata(docId: String, partId: UUID, title: String, source: Option[String]) = db.withTransaction { sql =>
    val rowsAffected = sql.update(DOCUMENT_FILEPART)
      .set(DOCUMENT_FILEPART.TITLE, title)
      .set(DOCUMENT_FILEPART.SOURCE, optString(source))
      .where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(docId).and(DOCUMENT_FILEPART.ID.equal(partId)))
      .execute()
      
    rowsAffected == 1
  }
  
  /** Changes the sequence numbers of fileparts for a specific document **/
  def setFilepartSortOrder(docId: String, sortOrder: Seq[PartOrdering]) = db.withTransaction { sql =>
    // To verify validaty of the request, load the fileparts from the DB first...
    val fileparts = 
      sql.selectFrom(DOCUMENT_FILEPART).where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(docId)).fetchArray()
    
    // ...discard parts that are not associated with the document and log a warning
    val foundIds = fileparts.map(_.getId).toSet
    val requestedIds = sortOrder.map(_.partId).toSet
    if (requestedIds != foundIds)
      Logger.warn("Attempt to re-order fileparts that don't belong to the specified doc")
    val sanitizedOrder = sortOrder.filter(ordering => foundIds.contains(ordering.partId))
    
    // Should normally be empty
    val unchangedParts = fileparts.filter(part => !requestedIds.contains(part.getId))
    if (unchangedParts.size > 0)
      Logger.warn("Request for re-ordering fileparts is missing " + unchangedParts.size + " rows")
   
    // There is no uniquness constraint in the DB on (documentId, seqNo), since we wouldn't be able to
    // update sequence numbers without changing part IDs then. Therefore we enforce uniqueness here.
    val updatedSequenceNumbers = sanitizedOrder.map(_.seqNo) ++ unchangedParts.map(_.getSequenceNo)
    if (updatedSequenceNumbers.size != updatedSequenceNumbers.distinct.size)
      throw new Exception("Uniqueness constraint violated for Filepart (document_id, sequence_no)")
      
    // Update fileparts in DB
    val updates = sanitizedOrder.map(ordering =>
      sql.update(DOCUMENT_FILEPART)
         .set(DOCUMENT_FILEPART.SEQUENCE_NO, ordering.seqNo.asInstanceOf[java.lang.Integer])
         .where(DOCUMENT_FILEPART.ID.equal(ordering.partId)))

    sql.batch(updates:_*).execute()
  }
  
  /** Batch-retrieves the document records with the given IDs **/
  def findByIds(docIds: Seq[String]) = db.query { sql => 
    sql.selectFrom(DOCUMENT).where(DOCUMENT.ID.in(docIds)).fetchArray().toSeq
  }
  
  /** Batch-retrieves the document records with the given ID, plus the sharing policy with the given user **/
  def findByIdsWithSharingPolicy(docIds: Seq[String], sharedWith: String) = db.query { sql =>
    val results = sql.selectFrom(SHARING_POLICY
         .join(DOCUMENT)
         .on(SHARING_POLICY.DOCUMENT_ID.equal(DOCUMENT.ID)))
       .where(SHARING_POLICY.SHARED_WITH.equal(sharedWith))
       .and(DOCUMENT.ID.in(docIds))
       .fetchArray().toSeq
    
    results.map(r => (r.into(classOf[DocumentRecord]), r.into(classOf[SharingPolicyRecord])))
  }
  
  /** Retrieves a document record by its ID, along with access permissions for the given user **/
  def getDocumentRecord(id: String, loggedInUser: Option[String] = None) = db.query { sql =>
    loggedInUser match {
      case Some(user) => {
        val records = 
          sql.selectFrom(DOCUMENT
               .leftJoin(SHARING_POLICY)
               .on(DOCUMENT.ID.equal(SHARING_POLICY.DOCUMENT_ID))
               .and(SHARING_POLICY.SHARED_WITH.equal(user)))
             .where(DOCUMENT.ID.equal(id))
             .fetchArray
             
        val grouped = groupLeftJoinResult(records, classOf[DocumentRecord], classOf[SharingPolicyRecord])
        if (grouped.size > 1)
          throw new RuntimeException("Got " + grouped.size + " DocumentRecords with the same ID: " + grouped.keys.map(_.getId).mkString(", "))
                      
        grouped.headOption.map { case (document, sharingPolicies) =>
          (document, determineAccessLevel(document, sharingPolicies, loggedInUser)) }
      }
      
      case None =>
        // Anonymous request - just retrieve document
        Option(sql.selectFrom(DOCUMENT).where(DOCUMENT.ID.equal(id)).fetchOne()).map(document =>
          (document, determineAccessLevel(document, Seq.empty[SharingPolicyRecord], loggedInUser)))
    }
  }

  /** Retrieves the document record, filepart metadata and owner information, along with access permissions **/
  def getExtendedInfo(id: String, loggedInUser: Option[String] = None) = db.query { sql =>
    val records = loggedInUser match {
      case Some(username) =>
        // Retrieve with sharing policies that may apply
        sql.selectFrom(DOCUMENT
             .join(DOCUMENT_FILEPART).on(DOCUMENT.ID.equal(DOCUMENT_FILEPART.DOCUMENT_ID))
             .join(USER).on(DOCUMENT.OWNER.equal(USER.USERNAME))
             .leftJoin(SHARING_POLICY)
               .on(DOCUMENT.ID.equal(SHARING_POLICY.DOCUMENT_ID))
               .and(SHARING_POLICY.SHARED_WITH.equal(username)))
          .where(DOCUMENT.ID.equal(id))
          .fetchArray()

      case None =>
        // Anyonymous request - just retrieve parts and owner
        sql.selectFrom(DOCUMENT
             .join(DOCUMENT_FILEPART).on(DOCUMENT.ID.equal(DOCUMENT_FILEPART.DOCUMENT_ID))
             .join(USER).on(DOCUMENT.OWNER.equal(USER.USERNAME)))
           .where(DOCUMENT.ID.equal(id))
           .fetchArray()

    }

    val grouped = groupLeftJoinResult(records, classOf[DocumentRecord], classOf[DocumentFilepartRecord])
    if (grouped.size > 1)
      throw new RuntimeException("Got " + grouped.size + " DocumentRecords with the same ID: " + grouped.keys.map(_.getId).mkString(", "))

    val sharingPolicies = records.map(_.into(classOf[SharingPolicyRecord])).filter(isNotNull(_)).distinct

    // Return with parts sorted by sequence number
    grouped.headOption.map { case (document, parts) =>
      val owner = records.head.into(classOf[UserRecord])
      (DocumentInfo(document, parts.sortBy(_.getSequenceNo), owner), determineAccessLevel(document, sharingPolicies, loggedInUser))
    }
  }
  
  /** Retrieves a part record by ID **/
  def findPartById(id: UUID) = db.query { sql => 
    Option(sql.selectFrom(DOCUMENT_FILEPART).where(DOCUMENT_FILEPART.ID.equal(id)).fetchOne())
  }

  /** Retrieves a filepart by document ID and sequence number **/
  def findPartByDocAndSeqNo(docId: String, seqNo: Int) = db.query { sql =>
    Option(sql.selectFrom(DOCUMENT_FILEPART)
              .where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(docId))
              .and(DOCUMENT_FILEPART.SEQUENCE_NO.equal(seqNo))
              .fetchOne())
  }
  
  def countByOwner(owner: String, publicOnly: Boolean = false) = db.query { sql =>
    if (publicOnly)
      sql.selectCount().from(DOCUMENT).where(DOCUMENT.OWNER.equal(owner).and(DOCUMENT.IS_PUBLIC.equal(true))).fetchOne(0, classOf[Int])
    else
      sql.selectCount().from(DOCUMENT).where(DOCUMENT.OWNER.equal(owner)).fetchOne(0, classOf[Int])
  }
  
  /** Convenience method to list all document IDs owned by the given user **/
  def listAllIdsByOwner(owner: String) = db.query { sql => 
    sql.select(DOCUMENT.ID).from(DOCUMENT).where(DOCUMENT.OWNER.equal(owner)).fetch(0, classOf[String]).toSeq
  }
  
  /** Retrieves documents by their owner **/
  def findByOwner(owner: String, offset: Int = 0, limit: Int = 20, sortBy: Option[String] = None, sortOrder: Option[SortOrder] = None) = db.query { sql =>
    val startTime = System.currentTimeMillis

    val sortField = sortBy.flatMap(fieldname => getSortField(Seq(DOCUMENT), fieldname, sortOrder))

    val total =
      sql.selectCount().from(DOCUMENT).where(DOCUMENT.OWNER.equal(owner)).fetchOne(0, classOf[Int])
    
    val query =
      sql.selectFrom(DOCUMENT).where(DOCUMENT.OWNER.equal(owner))

    val items = sortField match {
      case Some(sort) => query.orderBy(sort).limit(limit).offset(offset).fetchArray().toSeq
      case None => query.limit(limit).offset(offset).fetchArray().toSeq
    }
    
    Page(System.currentTimeMillis - startTime, total, offset, limit, items)
  }
  
  /** Retrieves documents from a given owner visible to the given logged in user.
    *
    * If there is currently no logged in user, only public documents are returned.  
    */
  def findAccessibleDocuments(
      owner: String,
      loggedInUser: Option[String],
      offset: Int = 0, limit: Int = 20,
      sortBy: Option[String] = None,
      sortOrder: Option[SortOrder] = None
    ) = db.query { sql =>
        
    val startTime = System.currentTimeMillis
    
    // TODO sorting
        
    val queries = Seq(sql.selectCount(), sql.select())
      .map { q => loggedInUser match {
        
          case Some(loggedIn) =>
            q.from(DOCUMENT)
              .where(DOCUMENT.OWNER.equal(owner).and(
                DOCUMENT.ID.in(sql.select(SHARING_POLICY.DOCUMENT_ID).from(SHARING_POLICY).where(SHARING_POLICY.SHARED_WITH.equal(loggedIn)))
                  .or(DOCUMENT.IS_PUBLIC.equal(true))))
                 
          case None =>       
            q.from(DOCUMENT).where(DOCUMENT.OWNER.equal(owner).and(DOCUMENT.IS_PUBLIC.equal(true)))
            
        }
      }
    
    val total = queries(0).fetchOne(0, classOf[Int])   
    val items = queries(1).limit(limit).offset(offset).fetch().into(classOf[DocumentRecord])
    
    Page(System.currentTimeMillis - startTime, total, offset, limit, items) 
  }
    
  /** Deletes a document by its ID, along with filepart records and files **/
  def delete(document: DocumentRecord): Future[Unit] = db.withTransaction { sql =>
    // Delete sharing policies
    sql.deleteFrom(SHARING_POLICY)
       .where(SHARING_POLICY.DOCUMENT_ID.equal(document.getId))
       .execute()
    
    // Delete filepart records
    sql.deleteFrom(DOCUMENT_FILEPART)
       .where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(document.getId))
       .execute()
    
    // Delete document records
    sql.deleteFrom(DOCUMENT)
       .where(DOCUMENT.ID.equal(document.getId))
       .execute()
       
    // Delete files - note: some documents may not have local files (e.g. IIIF)  
    val maybeDocumentDir = uploads.getDocumentDir(document.getOwner, document.getId)
    if (maybeDocumentDir.isDefined)
      FileUtils.deleteDirectory(maybeDocumentDir.get)
  }
  
  def deleteByOwner(username: String)(implicit ctx: ExecutionContext) = db.withTransaction { sql =>
    // Delete sharing policies
    sql.deleteFrom(SHARING_POLICY)
       .where(SHARING_POLICY.DOCUMENT_ID.in(
         sql.select(DOCUMENT.ID).from(DOCUMENT).where(DOCUMENT.OWNER.equal(username))
       ))
       .execute()
    
    // Delete filepart records
    sql.deleteFrom(DOCUMENT_FILEPART)
       .where(DOCUMENT_FILEPART.DOCUMENT_ID.in(
         sql.select(DOCUMENT.ID).from(DOCUMENT).where(DOCUMENT.OWNER.equal(username))
       ))
       .execute()
       
    // Delete document records
    sql.deleteFrom(DOCUMENT)
       .where(DOCUMENT.OWNER.equal(username))
       .execute()
       
    // Delete files
    uploads.deleteUserDir(username)
  }
  
}

object DocumentService extends HasDate {
  
  implicit val documentRecordWrites: Writes[DocumentRecord] = (
    (JsPath \ "id").write[String] and
    (JsPath \ "owner").write[String] and
    (JsPath \ "uploaded_at").write[DateTime] and
    (JsPath \ "title").write[String] and
    (JsPath \ "author").writeNullable[String] and
    // TODO date_numeric
    (JsPath \ "date_freeform").writeNullable[String] and
    (JsPath \ "description").writeNullable[String] and
    (JsPath \ "language").writeNullable[String] and
    (JsPath \ "source").writeNullable[String] and
    (JsPath \ "edition").writeNullable[String] and
    (JsPath \ "is_public").write[Boolean]
  )(d => (
    d.getId,
    d.getOwner,
    new DateTime(d.getUploadedAt.getTime),
    d.getTitle,
    Option(d.getAuthor),
    // TODO date_numeric
    Option(d.getDateFreeform),
    Option(d.getDescription),
    Option(d.getLanguage),
    Option(d.getSource),
    Option(d.getEdition),
    d.getIsPublic
  ))

  implicit val documentFilepartRecordWrites: Writes[DocumentFilepartRecord] = (
    (JsPath \ "id").write[UUID] and
    (JsPath \ "title").write[String] and
    (JsPath \ "content_type").write[String] and
    (JsPath \ "file").write[String] and
    (JsPath \ "source").writeNullable[String]
  )(p => (
    p.getId,
    p.getTitle,
    p.getContentType,
    p.getFile,
    Option(p.getSource)
  ))
  
  implicit val metadataWrites: Writes[(DocumentRecord, Seq[DocumentFilepartRecord])] = (
    (JsPath).write[DocumentRecord] and
    (JsPath \ "parts").write[Seq[DocumentFilepartRecord]]
  )(tuple => (tuple._1, tuple._2))
  
}


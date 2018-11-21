package services.document

import collection.JavaConversions._
import java.io.{File, InputStream}
import java.nio.file.Files
import java.util.UUID
import javax.inject.{Inject, Singleton}
import services.{BaseService, HasDate, Page, SortOrder}
import services.generated.Tables._
import services.generated.tables.records._
import org.joda.time.DateTime
import org.jooq.Record
import org.apache.commons.io.FileUtils
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.{ExecutionContext, Future}
import storage.db.DB
import storage.uploads.Uploads

case class PartOrdering(partId: UUID, seqNo: Int)

case class AccessibleDocumentsCount(public: Long, shared: Option[Long]) {

  lazy val total = public + shared.getOrElse(0l)

}

/** TODO this source file is just huge. I wonder how we can split it up into different parts **/
@Singleton
class DocumentService @Inject() (uploads: Uploads, implicit val db: DB) 
  extends BaseService 
  with queries.FindInFolder
  with DocumentIdFactory 
  with DocumentPrefsService
  with SharingPolicies {
  
  /** Creates a new DocumentRecord from an UploadRecord **/
  private[services] def createDocumentFromUpload(upload: UploadRecord) =
    new DocumentRecord(
          generateRandomID(),
          upload.getOwner,
          upload.getCreatedAt,
          upload.getTitle,
          upload.getAuthor,
          null, // TODO date_numeric
          upload.getDateFreeform,
          upload.getDescription,
          upload.getLanguage,
          upload.getSource,
          upload.getEdition,
          upload.getLicense,
          null, // attribution
          PublicAccess.PRIVATE.toString, // public_visibility
          null) // public_access_level
  
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
    
  /** Changes the public visibility and access level setting in one go **/
  def setPublicAccessOptions(docId: String, visibility: PublicAccess.Visibility, accessLevel: Option[PublicAccess.AccessLevel] = None) =
    db.withTransaction { sql =>
      sql.update(DOCUMENT)
         .set(DOCUMENT.PUBLIC_VISIBILITY, visibility.toString)
         .set(DOCUMENT.PUBLIC_ACCESS_LEVEL, optString(accessLevel.map(_.toString)))
         .where(DOCUMENT.ID.equal(docId)).execute()
    }
  
  def setPublicAccessLevel(docId: String, accessLevel: Option[PublicAccess.AccessLevel]) =
    db.query { sql =>
      sql.update(DOCUMENT)
         .set(DOCUMENT.PUBLIC_ACCESS_LEVEL, optString(accessLevel.map(_.toString)))
         .where(DOCUMENT.ID.equal(docId)).execute()
    }
  
  /** Updates the user-defined metadata fields **/
  def updateMetadata(
      docId: String,
      title: String,
      author: Option[String],
      dateFreeform: Option[String], 
      description: Option[String],
      language: Option[String],
      source: Option[String],
      edition: Option[String],
      license: Option[String],
      attribution: Option[String]): Future[Boolean] = db.withTransaction { sql =>  
      
    val q = sql.update(DOCUMENT)
      .set(DOCUMENT.TITLE, title)
      .set(DOCUMENT.AUTHOR, optString(author))
      .set(DOCUMENT.DATE_FREEFORM, optString(dateFreeform))
      .set(DOCUMENT.DESCRIPTION, optString(description))
      .set(DOCUMENT.LANGUAGE, optString(language))
      .set(DOCUMENT.SOURCE, optString(source))
      .set(DOCUMENT.EDITION, optString(edition))
      .set(DOCUMENT.LICENSE, optString(license))
      .set(DOCUMENT.ATTRIBUTION, optString(attribution))

    // If the update sets the document to a non-open license, make sure is_public is set to false
    val hasNonOpenLicense = license.map(acronym =>
      License.fromAcronym(acronym).map(!_.isOpen).getOrElse(true)).getOrElse(true)
      
    val rowsAffected =
      if (hasNonOpenLicense)
        q.set(DOCUMENT.PUBLIC_VISIBILITY, PublicAccess.PRIVATE.toString)
         .set(DOCUMENT.PUBLIC_ACCESS_LEVEL, null.asInstanceOf[String])
         .where(DOCUMENT.ID.equal(docId))
         .execute()
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

  /** Batch-retrieves the document records with the given IDs, along with their fileparts **/
  def findByIdsWithParts(docIds: Seq[String]) = db.query { sql =>
    val subquery = sql.selectFrom(DOCUMENT).where(DOCUMENT.ID.in(docIds))

    val rows = 
      sql.select().from(subquery)
         .join(DOCUMENT_FILEPART)
         .on(subquery.field(DOCUMENT.ID)
           .equal(DOCUMENT_FILEPART.DOCUMENT_ID))
         .fetchArray

    val asMap = groupLeftJoinResult(rows, classOf[DocumentRecord], classOf[DocumentFilepartRecord])

    val asTuples = rows.map(_.into(classOf[DocumentRecord])).distinct.map { document => 
      val fileparts = asMap.get(document).get
      (document -> fileparts)
    }.toSeq

    // Ensure results are in the same order as docIds
    docIds.map(id => asTuples.find(_._1.getId == id).get)
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

  protected def collectSharedWithMeResults(results: Seq[Record]) = {
    val documentsAndPolicies = results.map { r =>
      val document = r.into(classOf[DocumentRecord])
      val sharingPolicy = r.into(classOf[SharingPolicyRecord])
      (document, sharingPolicy)
    }.distinct

    val fileparts = results.map(_.into(classOf[DocumentFilepartRecord])).toSeq

    documentsAndPolicies.map { t =>
      val parts = fileparts.filter(_.getDocumentId == t._1.getId)
      (t._1, t._2, parts)
    }
  }

  /** Combined version of the two queries above **/
  def findByIdsWithPartsAndSharingPolicy(docIds: Seq[String], sharedWith: String) = db.query { sql =>
    // We'll retrieve all fields except SHARING_POLICY.ID, since this
    // would cause ambiguity issues in the query further down the line
    val subqueryFields = { 
      DOCUMENT.fields() ++
      SHARING_POLICY.fields() 
    } filter { _ != SHARING_POLICY.ID }

    val subquery = sql.select(subqueryFields:_*).from(SHARING_POLICY
          .join(DOCUMENT)
          .on(SHARING_POLICY.DOCUMENT_ID.equal(DOCUMENT.ID)))
        .where(SHARING_POLICY.SHARED_WITH.equal(sharedWith))
        .and(DOCUMENT.ID.in(docIds))

    val results =
      sql.select().from(subquery)
         .join(DOCUMENT_FILEPART)
         .on(subquery.field(DOCUMENT.ID)
           .equal(DOCUMENT_FILEPART.DOCUMENT_ID))
         .fetchArray

    val unsorted = collectSharedWithMeResults(results)
    docIds.map(id => unsorted.find(_._1.getId == id).get)
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
  
  def countAllByOwner(owner: String, publicOnly: Boolean = false) = db.query { sql =>
    if (publicOnly)
      sql.selectCount().from(DOCUMENT)
         .where(DOCUMENT.OWNER.equal(owner)
         .and(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString)))
         .fetchOne(0, classOf[Int])
    else
      sql.selectCount().from(DOCUMENT)
         .where(DOCUMENT.OWNER.equal(owner))
         .fetchOne(0, classOf[Int])
  }
  
  /** List all document IDs owned by the given user **/
  def listAllIdsByOwner(owner: String) = db.query { sql =>
    sql.select(DOCUMENT.ID).from(DOCUMENT).where(DOCUMENT.OWNER.equal(owner)).fetch(0, classOf[String]).toSeq
  }

  /** List all document IDs owned by the given user in the given folder.
    *
    * If inFolder is None, IDs from the root folder will be returned.
    */
  def listIdsByOwnerInFolder(owner: String, inFolder: Option[UUID]) = db.query { sql =>
    val baseQuery =
      sql.select(DOCUMENT.ID)
         .from(DOCUMENT)
         .fullOuterJoin(FOLDER_ASSOCIATION)
         .on(DOCUMENT.ID.equal(FOLDER_ASSOCIATION.DOCUMENT_ID))   
         .where(DOCUMENT.OWNER.equal(owner)) 

    inFolder match {
      case Some(folderId) =>   
        baseQuery
          // .and(FOLDER_ASSOCIATION.FOLDER_ID.equal(folderId))
          .fetch(0, classOf[String]).toSeq
      
      case None =>
        baseQuery
          .and(FOLDER_ASSOCIATION.FOLDER_ID.isNull)
          .fetch(0, classOf[String]).toSeq
    }
  }

  /** Retrieves documents by their owner **/
  def findByOwner(
    owner: String, 
    inFolder: Option[UUID], 
    offset: Int = 0, 
    limit: Int = 20, 
    sortBy: Option[String] = None, 
    sortOrder: Option[SortOrder] = None
  )(implicit ctx: ExecutionContext) = {
    val startTime = System.currentTimeMillis
    val sortField = sortBy.flatMap(fieldname => getSortField(Seq(DOCUMENT), fieldname, sortOrder))

    for {
      total <- inFolder match {
                 case Some(folderId) => countInFolder(folderId)
                 case None => countInRootFolder(owner)
               }

      items <- db.query { sql => 
                val query = inFolder match {
                  case Some(folderId) =>
                    sql.select().from(DOCUMENT)
                       .fullOuterJoin(FOLDER_ASSOCIATION)
                       .on(DOCUMENT.ID.equal(FOLDER_ASSOCIATION.DOCUMENT_ID))
                       .where(DOCUMENT.OWNER.equal(owner)
                         .and(FOLDER_ASSOCIATION.FOLDER_ID.equal(folderId)))

                  case None =>
                    sql.select().from(DOCUMENT)
                       .fullOuterJoin(FOLDER_ASSOCIATION)
                       .on(DOCUMENT.ID.equal(FOLDER_ASSOCIATION.DOCUMENT_ID))
                       .where(DOCUMENT.OWNER.equal(owner)
                         .and(FOLDER_ASSOCIATION.FOLDER_ID.isNull))
                }

                sortField match {
                  case Some(sort) =>
                    query.orderBy(sort).limit(limit).offset(offset)
                         .fetch()
                         .into(classOf[DocumentRecord]).toSeq

                  case None => 
                    query.limit(limit).offset(offset)
                         .fetch()
                         .into(classOf[DocumentRecord]).toSeq
                }
              }
              
    } yield Page(System.currentTimeMillis - startTime, total, offset, limit, items)
  }
  
  def listAllAccessibleIds(owner: String, loggedInUser: Option[String]) = db.query { sql =>
    loggedInUser match {
      case Some(username) =>
        sql.select(DOCUMENT.ID)
           .from(DOCUMENT)
           .where(DOCUMENT.OWNER.equalIgnoreCase(owner)
             .and(DOCUMENT.ID.in(
               sql.select(SHARING_POLICY.DOCUMENT_ID)
                  .from(SHARING_POLICY)
                  .where(SHARING_POLICY.SHARED_WITH.equal(username))
             ).or(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString))))
           .fetch(0, classOf[String]).toSeq

      case None =>
        sql.select(DOCUMENT.ID)
           .from(DOCUMENT)
           .where(DOCUMENT.OWNER.equalIgnoreCase(owner)
             .and(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString)))
           .fetch(0, classOf[String]).toSeq
    }
  }
  
  /** Retrieves documents from a given owner, visible to the given logged in user.
    *
    * If there is currently no logged in user, only documents with public_visibility = PUBLIC are returned.  
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
             .where(DOCUMENT.OWNER.equalIgnoreCase(owner)
               .and(DOCUMENT.ID.in(
                  sql.select(SHARING_POLICY.DOCUMENT_ID).from(SHARING_POLICY)
                    .where(SHARING_POLICY.SHARED_WITH.equal(loggedIn))
               ).or(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString))))
                 
          case None =>       
            q.from(DOCUMENT)
              .where(DOCUMENT.OWNER.equal(owner)
                .and(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString)))
        }
      }
    
    val total = queries(0).fetchOne(0, classOf[Int])   
    val items = queries(1).limit(limit).offset(offset).fetch().into(classOf[DocumentRecord])
    
    Page(System.currentTimeMillis - startTime, total, offset, limit, items) 
  }

  /** Just the document retrieval query, no counting **/
  private def listAccessibleWithParts(
    owner: String,
    loggedInUser: Option[String],
    offset: Int,
    limit: Int,
    sortBy: Option[String],
    sortOrder: Option[SortOrder]
  ) = db.query { sql =>
    val sortField = sortBy.flatMap(fieldname => getSortField(Seq(DOCUMENT), fieldname, sortOrder))

    val subquery = (sortField, loggedInUser) match {
      // Sorted query with logged in visitor
      case (Some(f), Some(user)) =>
        sql.selectFrom(DOCUMENT)
          .where(DOCUMENT.OWNER.equalIgnoreCase(owner)
            .and(DOCUMENT.ID.in(
              sql.select(SHARING_POLICY.DOCUMENT_ID).from(SHARING_POLICY)
                 .where(SHARING_POLICY.SHARED_WITH.equal(user)))
            .or(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString))))
          .orderBy(f)
          .limit(limit)
          .offset(offset)

      // Unsorted query with logged in visitor
      case (None, Some(user)) =>
        sql.selectFrom(DOCUMENT)
          .where(DOCUMENT.OWNER.equalIgnoreCase(owner)
            .and(DOCUMENT.ID.in(
              sql.select(SHARING_POLICY.DOCUMENT_ID).from(SHARING_POLICY)
                 .where(SHARING_POLICY.SHARED_WITH.equal(user)))
            .or(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString))))
            .limit(limit)
            .offset(offset)

      // Anonymous sorted query
      case (Some(f), None) =>
        sql.selectFrom(DOCUMENT)
           .where(DOCUMENT.OWNER.equal(owner)
             .and(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString)))
             .orderBy(f)
             .limit(limit)
             .offset(offset)

      // Anonymous unsorted query
      case (None, None) =>
        sql.selectFrom(DOCUMENT)
           .where(DOCUMENT.OWNER.equal(owner)
             .and(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString)))
           .limit(limit)
           .offset(offset)
    }

    val rows = 
      sql.select().from(subquery)
         .join(DOCUMENT_FILEPART)
         .on(subquery.field(DOCUMENT.ID)
           .equal(DOCUMENT_FILEPART.DOCUMENT_ID))
         .fetchArray

    val asMap = groupLeftJoinResult(rows, classOf[DocumentRecord], classOf[DocumentFilepartRecord])

    rows.map(_.into(classOf[DocumentRecord])).distinct.map { document => 
      val fileparts = asMap.get(document).get
      (document -> fileparts)
    }.toSeq
  }

  def findAccessibleDocumentsWithParts(
    owner: String,
    loggedInUser: Option[String],
    offset: Int,
    limit: Int,
    sortBy: Option[String],
    sortOrder: Option[SortOrder]
  )(implicit ctx: ExecutionContext) = {
    val startTime = System.currentTimeMillis
    val fCount = countAccessibleDocuments(owner, loggedInUser)
    val fDocuments = listAccessibleWithParts(owner, loggedInUser, offset, limit, sortBy, sortOrder)

    val f = for {
      count <- fCount
      documents <- fDocuments 
    } yield (count, documents)

    f.map { case (count, documents) =>
      Page(System.currentTimeMillis - startTime, count.total, offset, limit, documents) 
    }
  }

  def countAccessibleDocuments(owner: String, accessibleTo: Option[String]) = db.query { sql =>
    val public =
      sql.selectCount()
         .from(DOCUMENT)
         .where(DOCUMENT.OWNER.equalIgnoreCase(owner)
           .and(DOCUMENT.PUBLIC_VISIBILITY.equal(PublicAccess.PUBLIC.toString)))
         .fetchOne(0, classOf[Long])  

    val shared = accessibleTo.map { username => 
      sql.selectCount()
         .from(DOCUMENT)
         .where(DOCUMENT.OWNER.equalIgnoreCase(owner)
           // Don't double-count public docs!
           .and(DOCUMENT.PUBLIC_VISIBILITY.notEqual(PublicAccess.PUBLIC.toString))
           .and(DOCUMENT.ID.in(
             sql.select(SHARING_POLICY.DOCUMENT_ID)
                .from(SHARING_POLICY)
                .where(SHARING_POLICY.SHARED_WITH.equal(username))
             ))
         ).fetchOne(0, classOf[Long])
    }

    AccessibleDocumentsCount(public, shared)
  }
  
  /** Lists users who have at least one document with visibility set to PUBLIC **/
  def listOwnersWithPublicDocuments(offset: Int = 0, limit: Int = 10000) = db.query { sql =>
    sql.select(DOCUMENT.OWNER).from(DOCUMENT)
      .where(DOCUMENT.PUBLIC_VISIBILITY
        .equal(PublicAccess.PUBLIC.toString))
      .groupBy(DOCUMENT.OWNER)
      .limit(limit)
      .offset(offset)
      .fetch().into(classOf[String])
      .toSeq
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
    (JsPath \ "license").writeNullable[String] and
    (JsPath \ "attribution").writeNullable[String] and
    (JsPath \ "public_visibility").write[String] and
    (JsPath \ "public_access_level").writeNullable[String]
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
    Option(d.getLicense),
    Option(d.getAttribution),
    d.getPublicVisibility,
    Option(d.getPublicAccessLevel)
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


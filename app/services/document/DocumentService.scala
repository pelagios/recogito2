package services.document

import collection.JavaConversions._
import java.io.{File, InputStream}
import java.nio.file.Files
import java.util.UUID
import javax.inject.{Inject, Singleton}
import services.{BaseService, HasDate, Page, SortOrder, PublicAccess}
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

/** TODO this source file is just huge. I wonder how we can split it up into different parts **/
@Singleton
class DocumentService @Inject() (
  implicit val uploads: Uploads, 
  implicit val db: DB
) extends BaseService 
  with read.DocumentReadOps
  with read.AccessibleDocumentOps
  with delete.DeleteOps
  with queries.InMyFolderAllIds
  with queries.InMyFolderSorted
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
         .where(DOCUMENT.ID.equal(docId)).execute() == 1
    }
  
  def setPublicAccessLevel(docId: String, accessLevel: Option[PublicAccess.AccessLevel]) =
    db.query { sql =>
      sql.update(DOCUMENT)
         .set(DOCUMENT.PUBLIC_ACCESS_LEVEL, optString(accessLevel.map(_.toString)))
         .where(DOCUMENT.ID.equal(docId)).execute() == 1
    }

  def setPublicVisibility(docId: String, visibility: PublicAccess.Visibility) = 
    db.query { sql => 
      sql.update(DOCUMENT)
        .set(DOCUMENT.PUBLIC_VISIBILITY, visibility.toString)
        .where(DOCUMENT.ID.equal(docId)).execute() == 1
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


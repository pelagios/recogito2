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

/** TODO this source file is just huge. I wonder how we can split it up into different parts **/
@Singleton
class DocumentService @Inject() (
  implicit val uploads: Uploads, 
  implicit val db: DB
) extends BaseService 
  with read.DocumentReadOps
  with read.FilepartReadOps
  with read.AccessibleDocumentOps
  with update.UpdateOps
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


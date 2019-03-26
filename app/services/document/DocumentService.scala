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
import org.jooq.{DSLContext, Record, SelectConditionStep, SelectForUpdateStep}
import org.apache.commons.io.FileUtils
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.{ExecutionContext, Future}
import storage.db.DB
import storage.uploads.Uploads

/** TODO Note: I'm in the progress of refactoring this file **/
@Singleton
class DocumentService @Inject() (
  implicit val uploads: Uploads, 
  implicit val db: DB
) extends BaseService 
  with create.CreateOps
  with read.DocumentReadOps
  with read.FilepartReadOps
  with read.ReadFromFolderOps
  with read.CollaboratorReadOps
  with read.SharedWithMeReadOps
  with read.AccessibleDocumentOps
  with update.DocumentUpdateOps
  with update.CollaboratorUpdateOps
  with delete.DeleteOps
  with DocumentIdFactory {
  
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

  /** List all document IDs owned by the given user **/
  def listAllIdsByOwner(owner: String) = db.query { sql =>
    sql.select(DOCUMENT.ID)
       .from(DOCUMENT)
       .where(DOCUMENT.OWNER.equal(owner))
       .fetch(0, classOf[String])
       .toSeq
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
          .and(FOLDER_ASSOCIATION.FOLDER_ID.equal(folderId))
          .fetch(0, classOf[String]).toSeq
      
      case None =>
        baseQuery
          .and(FOLDER_ASSOCIATION.FOLDER_ID.isNull)
          .fetch(0, classOf[String]).toSeq
    }
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

  /** Common boilerplate code for listInFolder and listInRoot **/
  private def list(
    basequery: (DSLContext => SelectConditionStep[Record]),
    offset: Int,
    limit: Int,
    sortBy: Option[String],
    sortOrder: Option[SortOrder]
  )(implicit ctx: ExecutionContext) = db.query { sql =>

    def withParts(sql: DSLContext, subquery: SelectForUpdateStep[Record]) =
      sql.select().from(subquery)
         .join(DOCUMENT_FILEPART)
         .on(subquery.field(DOCUMENT.ID)
           .equal(DOCUMENT_FILEPART.DOCUMENT_ID))
         .fetchArray()

    val sortField = sortBy.flatMap(fieldname => getSortField(Seq(DOCUMENT), fieldname, sortOrder))
    val rows = sortField match {
      case Some(f) => 
        withParts(sql, basequery(sql).orderBy(f).limit(limit).offset(offset))
      case None =>
        withParts(sql, basequery(sql).limit(limit).offset(offset))
    }

    val asMap = groupLeftJoinResult(rows, classOf[DocumentRecord], classOf[DocumentFilepartRecord])
    rows.map(_.into(classOf[DocumentRecord])).distinct.map { document => 
      val fileparts = asMap.get(document).get
      (document -> fileparts)
    }
  }

  def listInMyFolderWithParts(
    owner: String,
    folderId: UUID,
    offset: Int,
    limit: Int,
    sortBy: Option[String],
    sortOrder: Option[SortOrder]
  )(implicit ctx: ExecutionContext) = {
    val startTime = System.currentTimeMillis

    def basequery(sql: DSLContext) = 
      sql.select()
         .from(DOCUMENT)
         .fullOuterJoin(FOLDER_ASSOCIATION)
         .on(DOCUMENT.ID.equal(FOLDER_ASSOCIATION.DOCUMENT_ID))
         .where(FOLDER_ASSOCIATION.FOLDER_ID.equal(folderId)
           .and(DOCUMENT.OWNER.equal(owner)))

    for {
      total <- countDocumentsInFolder(folderId)
      items <- list(basequery, offset, limit, sortBy, sortOrder) 
    } yield Page(System.currentTimeMillis - startTime, total, offset, limit, items)
  }

  def listInRootFolderWithParts(
    owner: String, 
    offset: Int, 
    limit: Int,
    sortBy: Option[String],
    sortOrder: Option[SortOrder]
  )(implicit ctx: ExecutionContext) = {
    val startTime = System.currentTimeMillis

    def basequery(sql: DSLContext) = 
      sql.select()
         .from(DOCUMENT)
         .fullOuterJoin(FOLDER_ASSOCIATION)
         .on(DOCUMENT.ID.equal(FOLDER_ASSOCIATION.DOCUMENT_ID))
         .where(FOLDER_ASSOCIATION.FOLDER_ID.isNull
           .and(DOCUMENT.OWNER.equal(owner)))
  
    for {
      total <- countInRootFolder(owner)
      items <- list(basequery, offset, limit, sortBy, sortOrder) 
    } yield Page(System.currentTimeMillis - startTime, total, offset, limit, items)
  }

  /** Shorthand to simplify the typical API list access **/
  def listByOwnerAndFolder(
    owner: String,
    maybeFolder: Option[UUID],
    offset: Int,
    limit: Int,
    sortBy: Option[String],
    sortOrder: Option[SortOrder]
  )(implicit ctx: ExecutionContext) = maybeFolder match {
    case Some(folderId) => listInMyFolderWithParts(owner, folderId, offset, limit, sortBy, sortOrder)
    case None => listInRootFolderWithParts(owner, offset, limit, sortBy, sortOrder)
  }

  def findSharedWithPart(
    sharedWith: String,
    offset: Int, 
    limit: Int, 
    sortBy: Option[String] = None, 
    sortOrder: Option[SortOrder] = None
  ) = db.query { sql => 
    val startTime = System.currentTimeMillis
    val subquerySortField = sortBy.flatMap(fieldname => getSortField(Seq(DOCUMENT, SHARING_POLICY), fieldname, sortOrder))
    val total = sql.selectCount().from(SHARING_POLICY).where(SHARING_POLICY.SHARED_WITH.equal(sharedWith)).fetchOne(0, classOf[Int])
  
    // We'll retrieve all fields except SHARING_POLICY.ID, since this
    // would cause ambiguity issues in the query further down the line
    val subqueryFields = { 
      DOCUMENT.fields() ++
      SHARING_POLICY.fields() 
    } filter { _ != SHARING_POLICY.ID }

    val subquery = subquerySortField match {
      case Some(f) => 
        sql.select(subqueryFields:_*)
           .from(SHARING_POLICY
             .join(DOCUMENT)
             .on(SHARING_POLICY.DOCUMENT_ID.equal(DOCUMENT.ID)))
           .where(SHARING_POLICY.SHARED_WITH.equal(sharedWith))
           .orderBy(f)
           .limit(limit)
           .offset(offset)

      case None =>
        sql.select(subqueryFields:_*)
           .from(SHARING_POLICY
             .join(DOCUMENT)
             .on(SHARING_POLICY.DOCUMENT_ID.equal(DOCUMENT.ID)))
           .where(SHARING_POLICY.SHARED_WITH.equal(sharedWith))
           .limit(limit)
           .offset(offset)
    }

    // Subquery makes this a little tricky...
    val querySortfield = sortBy.flatMap { fieldname =>
      val field = Seq(DOCUMENT, SHARING_POLICY).flatMap(_.fields).find(_.getName.equalsIgnoreCase(fieldname))
      field.map { f => 
        if (sortOrder.getOrElse(SortOrder.ASC) == SortOrder.ASC)
          subquery.field(f).asc
        else 
          subquery.field(f).desc
      }
    }

    val rows = querySortfield match {
      case Some(f) =>
        sql.select()
           .from(subquery)
           .join(DOCUMENT_FILEPART)
           .on(subquery.field(SHARING_POLICY.DOCUMENT_ID).equal(DOCUMENT_FILEPART.DOCUMENT_ID))
           .orderBy(f)
           .fetchArray

      case None =>
        sql.select()
           .from(subquery)
           .join(DOCUMENT_FILEPART)
           .on(subquery.field(SHARING_POLICY.DOCUMENT_ID).equal(DOCUMENT_FILEPART.DOCUMENT_ID))
           .fetchArray
    }

    val grouped = collectSharedWithMeResults(rows)
    Page(System.currentTimeMillis - startTime, total, offset, limit, grouped)
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


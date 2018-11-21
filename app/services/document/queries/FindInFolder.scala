package services.document.queries

import java.util.UUID
import org.jooq.{DSLContext, Record, SelectConditionStep, SelectForUpdateStep}
import scala.concurrent.ExecutionContext
import services.{Page, SortOrder}
import services.document.DocumentService
import services.generated.Tables.{DOCUMENT, DOCUMENT_FILEPART, FOLDER_ASSOCIATION}
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}

trait FindInFolder { self: DocumentService =>

  /** The number of documents in the folder **/
  def countInFolder(folderId: UUID) = db.query { sql => 
    sql.selectCount()
       .from(FOLDER_ASSOCIATION)
       .where(FOLDER_ASSOCIATION.FOLDER_ID.equal(folderId))
       .fetchOne(0, classOf[Int])
  }

  /** The number of documents in the user's root folder **/
  def countInRootFolder(owner: String) = db.query { sql =>
    sql.selectCount()
       .from(DOCUMENT)
       .fullOuterJoin(FOLDER_ASSOCIATION)
       .on(DOCUMENT.ID.equal(FOLDER_ASSOCIATION.DOCUMENT_ID))
       .where(DOCUMENT.OWNER.equal(owner)
         .and(FOLDER_ASSOCIATION.FOLDER_ID.isNull))
       .fetchOne(0, classOf[Int])
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

  def listInFolderWithParts(
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
         .where(FOLDER_ASSOCIATION.FOLDER_ID.equal(folderId))

    for {
      total <- countInFolder(folderId)
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
    case Some(folderId) => listInFolderWithParts(folderId, offset, limit, sortBy, sortOrder)
    case None => listInRootFolderWithParts(owner, offset, limit, sortBy, sortOrder)
  }

}
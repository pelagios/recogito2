package services.document.read

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import services.{Page, SortOrder}
import services.document.DocumentService
import services.document.read.results.MyDocument
import services.generated.Tables.{DOCUMENT, FOLDER_ASSOCIATION, SHARING_POLICY}
import services.generated.tables.records.{DocumentRecord, SharingPolicyRecord}

/** Helper functions that read documents from specific folders **/
trait ReadFromFolderOps { self: DocumentService => 

  /** The number of documents in the given folder **/
  def countDocumentsInFolder(folderId: UUID) = db.query { sql => 
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

  /** Lists documents in this folder, along with current user acess permissions, if any 
    * 
    * Used in the context of batch-applying sharing/collaborator settings. 
    */
  def listDocumentsInFolder(folderId: UUID, loggedInAs: String) = db.query { sql => 
    sql.select().from(FOLDER_ASSOCIATION)
      .join(DOCUMENT).on(DOCUMENT.ID.equal(FOLDER_ASSOCIATION.DOCUMENT_ID))
      .leftOuterJoin(SHARING_POLICY).on(SHARING_POLICY.DOCUMENT_ID.equal(DOCUMENT.ID)
        .and(SHARING_POLICY.SHARED_WITH.equal(loggedInAs)))
      .where(FOLDER_ASSOCIATION.FOLDER_ID.equal(folderId))
      .fetchArray().toSeq
      .map { record => 
        val doc = record.into(classOf[DocumentRecord])
        val policy = record.into(classOf[SharingPolicyRecord])

        // If there is no policy stored, the record will still be there, but 
        // with all fields == null
        if (policy.getSharedWith == null)
          (doc, None)
        else 
          (doc, Some(policy))
      }
  }

  /** Shorthand to list documents in multiple folders.
    *
    * Used in the context of batch-applying sharing/collaborator settings. 
    */
  def listDocumentsInFolders(
    ids: Seq[UUID],
    loggedInAs: String
  )(implicit ctx: ExecutionContext) = Future.sequence {
    ids.map { id => 
      listDocumentsInFolder(id, loggedInAs).map { _.map { t => 
        (t._1, t._2, id)
      }}
    }
  } map { _.flatten }

  /** Lists documents in the owner's root folder, with filecount and content-type info.
    * 
    * Used in the context of directory list access.
    */
  private def listDocumentsInRootWithPartInfo(
    owner: String, 
    offset: Int, 
    limit: Int,
    maybeSortBy: Option[String],
    maybeSortOrder: Option[SortOrder]
  )(implicit ctx: ExecutionContext) = db.query { sql =>
    val startTime = System.currentTimeMillis

    val sortBy = maybeSortBy.flatMap(sanitizeField).getOrElse("document.uploaded_at")
    val sortOrder = maybeSortOrder.map(_.toString).getOrElse("desc")

    val query = 
      s"""
       SELECT 
         document.*,
         file_count,
         content_types
       FROM document
         LEFT OUTER JOIN folder_association 
           ON folder_association.document_id = document.id
         JOIN (
           SELECT
             count(*) AS file_count,
             array_agg(DISTINCT content_type) AS content_types,
             document_id
           FROM document_filepart
           GROUP BY document_id
         ) AS parts ON parts.document_id = document.id
       WHERE document.owner = ? AND folder_association.folder_id IS NULL
       ORDER BY ${sortBy} ${sortOrder}
       OFFSET ${offset} LIMIT ${limit};
       """

    val records = sql.resultQuery(query, owner).fetchArray.map(MyDocument.build)
    Page(System.currentTimeMillis - startTime, records.size, 0, records.size, records)
  }

  /** Lists documents in the given folder, with filecount and content-type info. 
    *
    * Used in the context of directory list access.
    */
  private def listDocumentsInFolderWithPartInfo(
    folderId: UUID,
    offset: Int,
    limit: Int,
    maybeSortBy: Option[String],
    maybeSortOrder: Option[SortOrder]
  )(implicit ctx: ExecutionContext) = db.query { sql =>
    val startTime = System.currentTimeMillis

    val sortBy = maybeSortBy.flatMap(sanitizeField).getOrElse("document.uploaded_at")
    val sortOrder = maybeSortOrder.map(_.toString).getOrElse("desc")

    val query = 
      s"""
       SELECT 
         document.*,
         file_count,
         content_types
       FROM document
         LEFT OUTER JOIN folder_association 
           ON folder_association.document_id = document.id
         JOIN (
           SELECT
             count(*) AS file_count,
             array_agg(DISTINCT content_type) AS content_types,
             document_id
           FROM document_filepart
           GROUP BY document_id
         ) AS parts ON parts.document_id = document.id
       WHERE folder_association.folder_id = ?
       ORDER BY ${sortBy} ${sortOrder}
       OFFSET ${offset} LIMIT ${limit};
       """

    val records = sql.resultQuery(query, folderId).fetchArray.map(MyDocument.build)
    Page(System.currentTimeMillis - startTime, records.size, 0, records.size, records)
  }

  /** Shorthand for document list access with optional folder ID **/
  def listByOwnerAndFolder(
    owner: String,
    maybeFolder: Option[UUID],
    offset: Int,
    limit: Int,
    sortBy: Option[String],
    sortOrder: Option[SortOrder]
  )(implicit ctx: ExecutionContext) = maybeFolder match {
    case Some(folderId) => listDocumentsInFolderWithPartInfo(folderId, offset, limit, sortBy, sortOrder)
    case None => listDocumentsInRootWithPartInfo(owner, offset, limit, sortBy, sortOrder)
  }

}
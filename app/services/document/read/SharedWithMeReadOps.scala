package services.document.read

import java.util.UUID
import org.jooq.Record
import collection.JavaConversions._
import scala.concurrent.Future
import services.{ContentType, Page, SortOrder}
import services.document.{DocumentService, SharedDocument}
import services.generated.Tables.SHARING_POLICY
import services.generated.tables.records.{DocumentRecord, SharingPolicyRecord}

trait SharedWithMeReadOps { self: DocumentService =>

  /** Returns the number of documents shared with a given user **/
  def countDocumentsSharedWithMe(sharedWith: String) = db.query { sql =>
    sql.selectCount()
       .from(SHARING_POLICY)
       .where(SHARING_POLICY.SHARED_WITH.equal(sharedWith))
       .fetchOne(0, classOf[Int])
  }
  
  /** Convenience method to list all document IDs shared with the given user **/
  def listAllIdsSharedWithMe(username: String) = db.query { sql => 
    sql.select(SHARING_POLICY.DOCUMENT_ID)
       .from(SHARING_POLICY)
       .where(SHARING_POLICY.SHARED_WITH.equal(username))
       .fetch(0, classOf[String]).toSeq
  }

  /** List documents shared with me (in the given folder, or on root level) **/
  def listDocumentsSharedWithMe(
    username: String, 
    folder: Option[UUID],
    offset: Int,
    limit: Int,
    maybeSortBy: Option[String],
    maybeSortOrder: Option[SortOrder]
  ): Future[Page[SharedDocument]] =
    db.query { sql => 

      val startTime = System.currentTimeMillis

      def asSharedDocument(record: Record) = {
        val document = record.into(classOf[DocumentRecord])
        val policy = record.into(classOf[SharingPolicyRecord])
        val fileCount = record.getValue("file_count", classOf[Integer]).toInt
        val contentTypes = 
          record
            .getValue("content_types", classOf[Array[String]])
            .toSeq
            .flatMap(ContentType.withName)

        SharedDocument(document, policy, fileCount, contentTypes)
      }

      val sortBy = maybeSortBy.getOrElse("document.uploaded_at")
      val sortOrder = maybeSortOrder.map(_.toString).getOrElse("desc")

      val query = folder match {
        case Some(folderId) =>
          // Shared documents in this folder
          val query = 
            s"""
             SELECT 
               document.*,
               sharing_policy.*,
               file_count,
               content_types
             FROM sharing_policy
               JOIN folder_association ON folder_association.document_id = sharing_policy.document_id
               JOIN document ON document.id = sharing_policy.document_id
               JOIN (
                 SELECT
                   count(*) AS file_count,
                   array_agg(DISTINCT content_type) AS content_types,
                   document_id
                 FROM document_filepart
                 GROUP BY document_id
               ) AS parts ON parts.document_id = document.id
             WHERE sharing_policy.shared_with = ?
               AND folder_association.folder_id = ?
             OFFSET ${offset} LIMIT ${limit};
             """
          sql.resultQuery(query, username, folderId)
          
        case None =>
          // Shared documents at root level
          val query = 
            s"""
             SELECT 
               document.*,
               sharing_policy.*,
               file_count,
               content_types
             FROM sharing_policy
               JOIN document ON sharing_policy.document_id = document.id
               LEFT OUTER JOIN folder_association ON folder_association.document_id = sharing_policy.document_id
               LEFT OUTER JOIN sharing_policy folder_policy ON folder_policy.folder_id = folder_association.folder_id
               LEFT JOIN (
                 SELECT
                   count(*) AS file_count,
                   array_agg(DISTINCT content_type) AS content_types,
                   document_id
                 FROM document_filepart
                 GROUP BY document_id
               ) AS parts ON parts.document_id = document.id
             WHERE sharing_policy.shared_with = ?
               AND folder_policy.shared_with IS NULL
             ORDER BY ${sortBy} ${sortOrder}
             OFFSET ${offset} LIMIT ${limit};
             """
          sql.resultQuery(query, username)
      }

      val records = query.fetchArray.map(asSharedDocument)
      Page(System.currentTimeMillis - startTime, records.size, 0, records.size, records)
    }

}
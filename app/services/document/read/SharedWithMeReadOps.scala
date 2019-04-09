package services.document.read

import java.util.UUID
import org.jooq.Record
import collection.JavaConversions._
import scala.concurrent.Future
import services.{ContentType, Page, SortOrder}
import services.document.DocumentService
import services.document.read.results.SharedDocument
import services.generated.Tables.{FOLDER_ASSOCIATION, SHARING_POLICY}
import services.generated.tables.records.{DocumentRecord, SharingPolicyRecord}

trait SharedWithMeReadOps { self: DocumentService =>

  /** Returns the number of documents shared with a given user **/
  def countDocumentsSharedWithMe(sharedWith: String) = db.query { sql =>
    sql.selectCount()
       .from(SHARING_POLICY)
       .where(SHARING_POLICY.SHARED_WITH.equal(sharedWith))
       .fetchOne(0, classOf[Int])
  }
  
  /** List all document IDs shared with the given user (optionally, in the given folder) **/
  def listIdsSharedWithMe(username: String, folder: Option[UUID]) = db.query { sql => 
    folder match {
      case Some(folderId) => 
        // In folder
        sql.select(SHARING_POLICY.DOCUMENT_ID)
          .from(SHARING_POLICY)
          .join(FOLDER_ASSOCIATION)
            .on(FOLDER_ASSOCIATION.DOCUMENT_ID.equal(SHARING_POLICY.DOCUMENT_ID))
          .where(FOLDER_ASSOCIATION.FOLDER_ID.equal(folderId)
            .and(SHARING_POLICY.DOCUMENT_ID.isNotNull)
            .and(SHARING_POLICY.SHARED_WITH.equal(username)))
          .fetch(0, classOf[String])
          .toSeq

      case None => 
        // Root
        val query = 
          """
          SELECT 
            sharing_policy.document_id
          FROM sharing_policy
          LEFT OUTER JOIN folder_association
            ON folder_association.document_id = sharing_policy.document_id
          LEFT OUTER JOIN sharing_policy folder_policy
            ON folder_policy.folder_id = folder_association.folder_id
          WHERE sharing_policy.document_id IS NOT NULL
            AND sharing_policy.shared_with = ?
            AND folder_policy.shared_with IS NULL
          """
        sql.resultQuery(query, username)
          .fetch(0, classOf[String])
          .toSeq
    }
  }

  /** List documents shared with me (in the given folder, or on root level) **/
  def listDocumentsSharedWithMe(
    username: String, 
    folder: Option[UUID],
    offset: Int,
    limit: Int,
    maybeSortBy: Option[String],
    maybeSortOrder: Option[SortOrder]
  ): Future[Page[SharedDocument]] = db.query { sql => 
      val startTime = System.currentTimeMillis

      val sortBy = maybeSortBy.flatMap(sanitizeField).getOrElse("document.uploaded_at")
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
               JOIN folder_association 
                 ON folder_association.document_id = sharing_policy.document_id
               JOIN document 
                 ON document.id = sharing_policy.document_id
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
             ORDER BY ${sortBy} ${sortOrder}
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
               JOIN document
                 ON sharing_policy.document_id = document.id
               LEFT OUTER JOIN folder_association 
                 ON folder_association.document_id = sharing_policy.document_id
               LEFT OUTER JOIN sharing_policy folder_policy 
                 ON folder_policy.folder_id = folder_association.folder_id
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

      val records = query.fetchArray.map(SharedDocument.build)
      Page(System.currentTimeMillis - startTime, records.size, 0, records.size, records)
    }

  def getDocsSharedWithMeById(docIds: Seq[String], sharedWith: String) = db.query { sql => 
    val idSet = docIds
      .flatMap(sanitizeDocId) // prevent injection attacks
      .map(id => s"'${id}'") // SQL quoting
      .mkString(",") // join

    val query = 
      s"""
       SELECT 
         document.*,
         sharing_policy.*,
         file_count,
         content_types
       FROM document
         JOIN sharing_policy 
           ON sharing_policy.document_id = document.id AND
              sharing_policy.shared_with = ?
         JOIN (
           SELECT
             count(*) AS file_count,
             array_agg(DISTINCT content_type) AS content_types,
             document_id
           FROM document_filepart
           GROUP BY document_id
         ) AS parts ON parts.document_id = document.id
       WHERE document.id IN (${idSet});
       """

    sql.resultQuery(query, sharedWith).fetchArray.map(SharedDocument.build).toSeq
  }

}
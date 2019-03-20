package services.folder

import java.util.{Date, UUID}
import java.sql.Timestamp
import org.jooq.Record
import scala.concurrent.Future
import services.{ContentType, Page, PublicAccess, SharingLevel}
import services.generated.Tables.{SHARING_POLICY, FOLDER_ASSOCIATION, DOCUMENT}
import services.generated.tables.records.{DocumentRecord, SharingPolicyRecord}

trait SharedFolderService { self: FolderService => 

  /** Returns the list of collaborators on this folder **/
  def getCollaborators(id: UUID) = db.query { sql => 
    sql.selectFrom(SHARING_POLICY)
       .where(SHARING_POLICY.FOLDER_ID.equal(id))
       .fetchArray
       .toSeq
  }

  def addCollaborator(folderId: UUID, sharedBy: String, sharedWith: String, level: SharingLevel) = 
    db.query { sql => 
      val existing = sql.selectFrom(SHARING_POLICY)
        .where(SHARING_POLICY.FOLDER_ID.equal(folderId)
          .and(SHARING_POLICY.SHARED_WITH.equal(sharedWith))).fetchOne 

      val policy = Option(existing) match {
        case Some(policy) =>
          policy.setSharedBy(sharedBy)
          policy.setSharedAt(new Timestamp(new Date().getTime))
          policy.setAccessLevel(level.toString)
          policy

        case None => 
          val policy = new SharingPolicyRecord(
            null, // auto-inc id
            folderId,
            null, // document_id
            sharedBy,
            sharedWith,
            new Timestamp(new Date().getTime),
            level.toString)

          policy.changed(SHARING_POLICY.ID, false)     
          sql.attach(policy)
          policy
      }
      
      policy.store() == 1
    }

  def removeCollaborator(folderId: UUID, sharedWith: String) =
    db.query { sql => 
      sql.deleteFrom(SHARING_POLICY)
         .where(SHARING_POLICY.FOLDER_ID.equal(folderId)
           .and(SHARING_POLICY.SHARED_WITH.equal(sharedWith)))
         .execute == 1
    } 

  def listDocumentsSharedWithMe(username: String, folder: Option[UUID]): Future[Page[SharedDocument]] =
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

      val query = folder match {
        case Some(folderId) =>
          // Shared documents in this folder
          val query = 
            """
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
              AND folder_association.folder_id = ?;
            """

            // TODO surface file count and content types list
            sql.resultQuery(query, username)
          
        case None =>
          // Shared documents at root level
          val query = 
            """
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
              AND folder_policy.shared_with IS NULL;
            """
          
          // TODO surface file count and content types list
          sql.resultQuery(query, username)
      }

      val records = query.fetchArray.map(asSharedDocument)
      Page(System.currentTimeMillis - startTime, records.size, 0, records.size, records)
    }

}
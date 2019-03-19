package services.folder

import java.util.{Date, UUID}
import java.sql.Timestamp
import org.jooq.Record
import services.{PublicAccess, SharingLevel}
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

  def listDocumentsSharedWithMe(username: String, folder: Option[UUID]) =
    db.query { sql => 

      // Helper
      def asTuple(record: Record) = {
        val document = record.into(classOf[DocumentRecord])
        val policy = record.into(classOf[SharingPolicyRecord])
        (document, policy)
      }

      folder match {
        case Some(folderId) =>
          // Shared documents in this folder
          sql.select().from(SHARING_POLICY)
             .join(FOLDER_ASSOCIATION)
               .on(FOLDER_ASSOCIATION.DOCUMENT_ID.equal(SHARING_POLICY.DOCUMENT_ID))
             .join(DOCUMENT)
               .on(DOCUMENT.ID.equal(SHARING_POLICY.DOCUMENT_ID))
             .where(SHARING_POLICY.SHARED_WITH.equal(username)
               .and(FOLDER_ASSOCIATION.FOLDER_ID.equal(folderId)))
             .fetchArray
             .map(asTuple)
          
        case None =>
          // Shared documents at root level
          val query = 
            """
            SELECT 
              sharing_policy.*,
              document.*,
              folder_policy.shared_with AS folder_shared
            FROM sharing_policy
              JOIN document ON sharing_policy.document_id = document.id
              LEFT OUTER JOIN folder_association ON folder_association.document_id = sharing_policy.document_id
              LEFT OUTER JOIN sharing_policy folder_policy ON folder_policy.folder_id = folder_association.folder_id
            WHERE sharing_policy.shared_with = ?
              AND folder_policy.shared_with IS NULL;
            """
          
          sql.resultQuery(query, username).fetchArray.map(asTuple)
      }
    }

}
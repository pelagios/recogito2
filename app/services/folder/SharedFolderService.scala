package services.folder

import java.util.{Date, UUID}
import java.sql.Timestamp
import org.jooq.Record
import scala.concurrent.Future
import services.{ContentType, Page, PublicAccess, SharingLevel}
import services.generated.Tables.{SHARING_POLICY, FOLDER_ASSOCIATION, DOCUMENT}
import services.generated.tables.records.{DocumentRecord, SharingPolicyRecord}

trait SharedFolderService { self: FolderService => 

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
    
}
package services.folder

import java.util.UUID
import org.jooq.DSLContext
import services.generated.Tables.{DOCUMENT, FOLDER_ASSOCIATION, SHARING_POLICY}
import services.generated.tables.records.{DocumentRecord, FolderAssociationRecord, SharingPolicyRecord}
import scala.concurrent.Future

trait FolderAssociationService { self: FolderService =>

  private def insertAssociation(documentId: String, folderId: UUID, sql: DSLContext) = {
    val association = new FolderAssociationRecord(folderId, documentId)
    sql.insertInto(FOLDER_ASSOCIATION).set(association).execute()
    association
  }

  /** Adds a document to a folder **/
  def addDocumentToFolder(documentId: String, folderId: UUID) = 
    db.withTransaction { sql => insertAssociation(documentId, folderId, sql) }

  /** Same as addToFolder, but removes existing association if any **/
  def moveDocumentToFolder(documentId: String, folderId: UUID) =
    db.withTransaction { sql => 
      sql.deleteFrom(FOLDER_ASSOCIATION)
         .where(FOLDER_ASSOCIATION.DOCUMENT_ID.equal(documentId))
         .execute

      insertAssociation(documentId, folderId, sql)
    }

  /** Deletes all associations for this document **/
  def deleteFolderAssociations(documentId: String) = 
    db.withTransaction { sql => 
      sql.deleteFrom(FOLDER_ASSOCIATION)
         .where(FOLDER_ASSOCIATION.DOCUMENT_ID.equal(documentId))
         .execute
    }

  /** Lists documents in this folder, along with current user acess permissions, if any */
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

}
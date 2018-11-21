package services.folder

import java.util.UUID
import org.jooq.DSLContext
import services.generated.Tables.FOLDER_ASSOCIATION
import services.generated.tables.records.FolderAssociationRecord
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

}
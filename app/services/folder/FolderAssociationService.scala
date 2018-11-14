package services.folder

import services.generated.Tables.FOLDER_ASSOCIATION
import services.generated.tables.records.FolderAssociationRecord
import scala.concurrent.Future

trait FolderAssociationService { self: FolderService =>

  /** Adds a document to a folder **/
  def addDocumentToFolder(documentId: String, folderId: Int) = 
    db.withTransaction { sql => ??? }

  /** Same as addToFolder, but removes existing association if any **/
  def moveDocumentToFolder(documentId: String, folderId: Int) =
    db.withTransaction { sql => ??? }

  /** Deletes all associations for this document **/
  def deleteFolderAssociations(documentId: String) = 
    db.withTransaction { sql => 
      sql.deleteFrom(FOLDER_ASSOCIATION)
         .where(FOLDER_ASSOCIATION.DOCUMENT_ID.equal(documentId))
         .execute
    }

}
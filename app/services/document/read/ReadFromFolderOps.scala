package services.document.read

import java.util.UUID
import services.document.DocumentService
import services.generated.Tables.{DOCUMENT, FOLDER_ASSOCIATION}

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

}
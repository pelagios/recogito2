package services.document.queries

import collection.JavaConversions._
import java.util.UUID
import services.document.DocumentService
import services.generated.Tables.{DOCUMENT, FOLDER_ASSOCIATION}

trait InMyFolderAllIds { self: DocumentService =>

  /** List all document IDs owned by the given user **/
  def listAllIdsByOwner(owner: String) = db.query { sql =>
    sql.select(DOCUMENT.ID)
       .from(DOCUMENT)
       .where(DOCUMENT.OWNER.equal(owner))
       .fetch(0, classOf[String])
       .toSeq
  }
 
  /** List all document IDs owned by the given user in the given folder.
    *
    * If inFolder is None, IDs from the root folder will be returned.
    */
  def listIdsByOwnerInFolder(owner: String, inFolder: Option[UUID]) = db.query { sql =>
    val baseQuery =
      sql.select(DOCUMENT.ID)
         .from(DOCUMENT)
         .fullOuterJoin(FOLDER_ASSOCIATION)
         .on(DOCUMENT.ID.equal(FOLDER_ASSOCIATION.DOCUMENT_ID))   
         .where(DOCUMENT.OWNER.equal(owner)) 

    inFolder match {
      case Some(folderId) =>   
        baseQuery
          .and(FOLDER_ASSOCIATION.FOLDER_ID.equal(folderId))
          .fetch(0, classOf[String]).toSeq
      
      case None =>
        baseQuery
          .and(FOLDER_ASSOCIATION.FOLDER_ID.isNull)
          .fetch(0, classOf[String]).toSeq
    }
  }

}
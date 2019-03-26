package services.folder.delete

import java.util.UUID
import scala.concurrent.Future
import services.folder.FolderService
import services.generated.Tables.FOLDER

trait DeleteOps { self: FolderService => 

  def deleteFolder(id: UUID): Future[Boolean] = 
    db.withTransaction { sql => 
      sql.deleteFrom(FOLDER).where(FOLDER.ID.equal(id)).execute == 1
    }

  def deleteFolderByOwner(owner: String): Future[Boolean] = 
    db.withTransaction { sql => 
      sql.deleteFrom(FOLDER).where(FOLDER.OWNER.equal(owner)).execute == 1
    }

}
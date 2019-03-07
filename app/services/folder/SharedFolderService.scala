package services.folder

import java.util.UUID
import services.PublicAccess

trait SharedFolderService { self: FolderService => 

  /** Returns the list of collaborators on this folder **/
  def getCollaborators(id: UUID) = db.query { sql => 

    ???
    
  }

  def addCollaborator(ids: Seq[UUID], username: String, accessLevel: PublicAccess.AccessLevel) = 
    db.query { sql => 

      ???
    }

}
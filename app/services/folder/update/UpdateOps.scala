package services.folder.update

import java.util.UUID

import services.PublicAccess
import services.folder.FolderService
import services.generated.Tables.FOLDER

trait UpdateOps { self: FolderService => 

  def renameFolder(id: UUID, title: String) = 
    db.withTransaction { sql => 
      sql.update(FOLDER)
        .set(FOLDER.TITLE, title)
        .where(FOLDER.ID.equal(id))
        .execute() == 1
    }

  def updatePublicVisibility(id: UUID, value: PublicAccess.Visibility) = db.withTransaction { sql =>
    sql.update(FOLDER)
       .set(FOLDER.PUBLIC_VISIBILITY, value.toString)
       .where(FOLDER.ID.equal(id))
       .execute == 1
  }

  def updatePublicAccessLevel(id: UUID, value: PublicAccess.AccessLevel) = db.withTransaction { sql => 
    sql.update(FOLDER)
       .set(FOLDER.PUBLIC_ACCESS_LEVEL, value.toString)
       .where(FOLDER.ID.equal(id))
       .execute == 1
  }

  def updateVisibilitySettings(
    id: UUID,
    visibility: PublicAccess.Visibility, 
    accessLevel: Option[PublicAccess.AccessLevel]
  ) = db.withTransaction { sql => 
    sql.update(FOLDER)
       .set(FOLDER.PUBLIC_VISIBILITY, visibility.toString)
       .set(FOLDER.PUBLIC_ACCESS_LEVEL, optString(accessLevel.map(_.toString)))
       .where(FOLDER.ID.equal(id))
       .execute == 1
  }

  def updateReadme(id: UUID, readme: String) =
    db.withTransaction { sql =>
      sql.update(FOLDER)
         .set(FOLDER.README, readme)
         .where(FOLDER.ID.equal(id))
         .execute == 1
    }

}
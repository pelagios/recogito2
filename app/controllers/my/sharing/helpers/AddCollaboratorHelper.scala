package controllers.my.sharing.helpers

import java.util.UUID
import scala.concurrent.Future
import services.SharingLevel
import services.folder.FolderService

trait AddCollaboratorHelper {

  private def addCollaboratorToOneFolder = ???

  private def addCollaboratorToOneDocument = ???

  private def addCollaboratorToFolderList = ???

  private def addCollaboratorToDocumentList = ???

  def addCollaboratorRecursive(
    folderId: UUID,
    loggedInAs: String,
    collaborator: String,
    level: SharingLevel
  )(implicit
      folderService: FolderService
  ): Future[Boolean] = {

    // TODO placeholder - this is just the folder. Not recursive, no nested documents
    folderService.addCollaborator(
      folderId, loggedInAs, collaborator, level
    )

  }

}
package controllers.my.sharing.helpers

import java.util.UUID
import scala.concurrent.Future
import services.SharingLevel
import services.SharingLevel.Utils._
import services.folder.FolderService
import services.generated.tables.records.{FolderRecord, SharingPolicyRecord}

trait AddCollaboratorHelper {

  private def addCollaboratorToOneFolder(
    folder: FolderRecord,
    policy: Option[SharingPolicyRecord],
    loggedInAs: String,
    collaborator: String,
    level: SharingLevel
  )(implicit folderService: FolderService): Future[Boolean] = {
    if (isFolderAdmin(loggedInAs, folder, policy)) {
      folderService.addCollaborator(folder.getId, loggedInAs, collaborator, level)
    } else {
      Future.successful(false)
    }
  }

  /*
    private def applyVisibilityToOneFolder(
    folder: FolderRecord, 
    policy: Option[SharingPolicyRecord],
    loggedInAs: String,
    visibility: Option[PublicAccess.Visibility],
    accessLevel: Option[PublicAccess.AccessLevel]
  )(implicit 
      folderService: FolderService,
      ctx: ExecutionContext
  ): Future[Boolean] = {
    if (isFolderAllowed(folder, policy, loggedInAs)) {
      val fUpdateVisibility = visibility.map { v => 
        folderService.updatePublicVisibility(folder.getId, v)
      }

      val fUpdateAccessLevel = accessLevel.map { a => 
        folderService.updatePublicAccessLevel(folder.getId, a)
      }

      Future.sequence {
        Seq(fUpdateVisibility, fUpdateAccessLevel).flatten
      } map { !_.exists(_ == false) }
    } else {
      Future.successful(false)
    }
  }
  */

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
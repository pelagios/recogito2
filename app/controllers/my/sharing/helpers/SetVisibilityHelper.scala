package controllers.my.sharing.helpers

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import services.{PublicAccess, SharingLevel}
import services.document.DocumentService
import services.folder.FolderService
import services.generated.tables.records.{DocumentRecord, SharingPolicyRecord}

trait SetVisibilityHelper {

  /** Helper that does the actual work of applying the visibility setting, if allowed.
    * 
    * Returns Future[false] when the setting was not applied due to access 
    * restrictions, Future[true] otherwise.
    */
  private def applyVisibilityIfAllowed(
    doc: DocumentRecord, 
    policy: Option[SharingPolicyRecord], 
    loggedInAs: String,
    visibility: PublicAccess.Visibility,
    accessLevel: PublicAccess.AccessLevel
  )(
    implicit documentService: DocumentService
  ): Future[Boolean] = {
    val isAllowed = 
      doc.getOwner == loggedInAs || // document owner OR
      policy.map(_.getAccessLevel == SharingLevel.ADMIN).getOrElse(false) // Has admin rights

    if (isAllowed)
      documentService.setPublicAccessOptions(doc.getId, visibility, Some(accessLevel))
    else
      Future.successful(false) // don't execute
  }

  /** Applies visibility settings to all documents in the given folder.
    * 
    * This method is NOT recursive. Documents in sub-folders of this folder
    * will not be affected. Returns Future[false] if there was any documents where
    * the setting could not be applied due to access restrictions, Future[true]
    * otherwise.
    */
  private def setDocumentVisiblity(
    folderId: UUID,
    loggedInAs: String,
    visibility: PublicAccess.Visibility,
    accessLevel: PublicAccess.AccessLevel
  )(implicit 
      documentService: DocumentService, 
      folderService: FolderService, 
      ctx: ExecutionContext
  ): Future[Boolean] =  {
    folderService.listDocumentsInFolder(folderId, loggedInAs)
      .flatMap { result => 
        Future.sequence {
          result.map(t => applyVisibilityIfAllowed(t._1, t._2, loggedInAs, visibility, accessLevel))
        } map { !_.exists(_ == false) }
      }
  }

  /** Applies visibility settings to all documents in this folder and all nested folders.
    * 
    * This method is NOT recursive. Documents in sub-folders of this folder
    * will not be affected. Returns Future[false] if there was any documents where
    * the setting could not be applied due to access restrictions, Future[true]
    * otherwise.
    */
  def setDocumentVisibilityRecursive(
    folderId: UUID,
    loggedInAs: String,
    visibility: PublicAccess.Visibility,
    accessLevel: PublicAccess.AccessLevel
  )(implicit
      documentService: DocumentService,
      folderService: FolderService,
      ctx: ExecutionContext
  ): Future[Boolean] = {
    folderService.getChildrenRecursive(folderId).flatMap { nestedFolders =>
      Future.sequence {
        nestedFolders.map { case (id, title) => 
          setDocumentVisiblity(id, loggedInAs, visibility, accessLevel) 
        }
      } map { !_.exists(_ == false) }
    }
  }

}
package controllers.my.sharing.helpers

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import services.{PublicAccess, SharingLevel}
import services.document.DocumentService
import services.folder.FolderService
import services.generated.tables.records.{DocumentRecord, FolderRecord, SharingPolicyRecord}

/** TODO in theory, we can optimize these queries a lot, through batching 
  * the DB queries. In practice, however, benefit is probably minimal
  * as users will usually not apply new settings to hundreds of 
  * nested documents and subfolders. 
  */
trait SetVisibilityHelper {

  /** Does the actual work of applying the visibility setting to 
    * a single FolderRecord, if permissions allow.
    * 
    * Returns Future(false) when the setting was not applied due to
    * access restrictions, Future(true) otherwise.
    */
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
    val isAllowed = 
      folder.getOwner == loggedInAs || // folder owner OR
      policy.map(_.getAccessLevel == SharingLevel.ADMIN).getOrElse(false)

    if (isAllowed) {
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

  /** Does the actual work of applying the visibility setting
    * to a single DocumentRecord, if permissions allow.
    * 
    * Returns Future(false) when the setting was not applied due to access 
    * restrictions, Future(true) otherwise.
    */
  private def applyVisibilityToOneDocument(
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

  /** Applies visibility settings to all documents in the folder with the given ID.
    * 
    * This method is NOT recursive. Documents in sub-folders of this folder
    * will not be affected. Returns Future(false) if there was any documents where
    * the setting could not be applied due to access restrictions, Future(true)
    * otherwise.
    */
  private def applyVisibilityToDocumentsList(
    folderId: UUID,
    loggedInAs: String,
    visibility: PublicAccess.Visibility,
    accessLevel: PublicAccess.AccessLevel
  )(implicit 
      documentService: DocumentService, 
      folderService: FolderService, 
      ctx: ExecutionContext
  ): Future[Boolean] = {
    folderService.listDocumentsInFolder(folderId, loggedInAs)
      .flatMap { result => 
        Future.sequence {
          result.map(t => applyVisibilityToOneDocument(t._1, t._2, loggedInAs, visibility, accessLevel))
        } map { !_.exists(_ == false) }
      }
  }

  /** Applies visibility settings to the list of folders. This method is not
    * recursive.
    *
    * Returns Future(false) if there was any folder or document where the
    * setting could not be applied due to access restrictions, Future(true)
    * otherwise.
    */
  private def applyVisibilityToFolderList(
    folderIds: Seq[UUID],
    loggedInAs: String,
    visibility: Option[PublicAccess.Visibility],
    accessLevel: Option[PublicAccess.AccessLevel]
  )(implicit
      folderService: FolderService,
      ctx: ExecutionContext
  ): Future[Boolean] = {
    folderService.getFolders(folderIds, loggedInAs).flatMap { result => 
      Future.sequence {
        result.map(t => applyVisibilityToOneFolder(t._1, t._2, loggedInAs, visibility, accessLevel))
      } map { !_.exists(_ == false) }
    }
  }

  /** Applies visibility settings to all folders, documents and nested items. 
    *
    * Returns Future(false) if there was any folder or document where the
    * setting could not be applied due to access restrictions, Future(true)
    * otherwise.
    */
  def setVisibilityRecursive(
    folderId: UUID,
    loggedInAs: String,
    visibility: Option[PublicAccess.Visibility],
    accessLevel: Option[PublicAccess.AccessLevel]
  )(implicit
      folderService: FolderService,
      ctx: ExecutionContext 
  ): Future[Boolean] = {

    // TODO handle nested documents

    folderService.getChildrenRecursive(folderId).flatMap { children => 
      val allFolders = folderId +: children.map(_._1)
      applyVisibilityToFolderList(allFolders, loggedInAs, visibility, accessLevel)
    }
  }

}
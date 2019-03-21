package controllers.my.sharing.helpers

import AccessibleItems.Utils._
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import services.{PublicAccess, SharingLevel}
import services.SharingLevel.Utils._
import services.document.DocumentService
import services.folder.FolderService
import services.generated.tables.records.{DocumentRecord, FolderRecord, SharingPolicyRecord}

/** TODO in theory, we can optimize these queries a lot, through batching 
  * the DB queries. In practice, however, benefit is probably minimal
  * as users will usually not apply new settings to hundreds of 
  * nested documents and subfolders. 
  */
trait SetVisibilityHelper {

  /** Does the actual work of applying the visibility setting 
    * to a single FolderRecord, if permissions allow.
    * 
    * Returns Future(false) when the setting was not applied 
    * due to access restrictions, Future(true) otherwise.
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
    if (isFolderAdmin(loggedInAs, folder, policy)) {
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
    * Returns Future(false) when the setting was not applied 
    * due to access restrictions, Future(true) otherwise.
    */
  private def applyVisibilityToOneDocument(
    doc: DocumentRecord, 
    policy: Option[SharingPolicyRecord], 
    loggedInAs: String,
    visibility: Option[PublicAccess.Visibility],
    accessLevel: Option[PublicAccess.AccessLevel]
  )(implicit
      documentService: DocumentService,
      ctx: ExecutionContext
  ): Future[Boolean] = {
    if (isDocumentAdmin(loggedInAs, doc, policy)) {
      val fSetVisiblity = visibility.map(v => documentService.setPublicVisibility(doc.getId, v))
      val fSetAccessLevel = accessLevel.map(l => documentService.setPublicAccessLevel(doc.getId, Some(l)))

      Future.sequence {
        Seq(fSetVisiblity, fSetAccessLevel).flatten
      } map { !_.exists(_ == false) }
    } else {
      Future.successful(false) // don't execute
    }
  }

  /** Applies visibility settings to the list of folders. This 
    * method is not recursive.
    *
    * Returns Future(false) if there was any folder where the
    * setting could not be applied due to access restrictions, 
    * Future(true) otherwise.
    */
  private def applyVisibilityToFolderList(
    folders: Seq[(FolderRecord, Option[SharingPolicyRecord])],
    loggedInAs: String,
    visibility: Option[PublicAccess.Visibility],
    accessLevel: Option[PublicAccess.AccessLevel]
  )(implicit
      folderService: FolderService,
      ctx: ExecutionContext
  ): Future[Boolean] = {
    Future.sequence {
      folders.map(t => applyVisibilityToOneFolder(t._1, t._2, loggedInAs, visibility, accessLevel))
    } map { !_.exists(_ == false) }
  }

  /** Applies visibility settings to all documents in the list.
    * 
    * Returns Future(false) if there was any document where the
    * setting could not be applied due to access restrictions, 
    * Future(true) otherwise.
    */
  private def applyVisibilityToDocumentsList(
    documents: Seq[(DocumentRecord, Option[SharingPolicyRecord])],
    loggedInAs: String,
    visibility: Option[PublicAccess.Visibility],
    accessLevel: Option[PublicAccess.AccessLevel]
  )(implicit 
      documentService: DocumentService, 
      ctx: ExecutionContext
  ): Future[Boolean] = Future.sequence {
    documents.map(t => applyVisibilityToOneDocument(t._1, t._2, loggedInAs, visibility, accessLevel))
  } map { !_.exists(_ == false) }

  /** Applies visibility settings to the given folder, the documents 
    * inside it, and recursively all sub-folders and their documents. 
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
      documentService: DocumentService,
      ctx: ExecutionContext 
  ): Future[Boolean] = {

    val accessibleItems = listAccessibleItemsRecursive(folderId, loggedInAs)
    accessibleItems.flatMap { items => 
      // Apply folder visibility (permissions are being checked folder-by-folder)
      val fFolderSuccess = applyVisibilityToFolderList(items.folders, loggedInAs, visibility, accessLevel)

      // Apply document visibility in allowed folders (document permissions are being checked case by case)
      val fDocumentSuccess = applyVisibilityToDocumentsList(items.documents, loggedInAs, visibility, accessLevel)
      
      for {
        folderSuccess <- fFolderSuccess
        documentSuccess <- fDocumentSuccess
      } yield (folderSuccess && documentSuccess)
    }
  }

}
package controllers.my.sharing.helpers

import AccessibleItems.Utils._
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import services.SharingLevel.Utils._
import services.document.DocumentService
import services.folder.FolderService
import services.generated.tables.records.{DocumentRecord, FolderRecord, SharingPolicyRecord}

trait RemoveCollaboratorHelper {

  /** Does the actual work of removing a collaborator from 
    * a single FolderRecord, if permissions allow.
    * 
    * Returns Future(false) when the collaborator was not removed
    * due to access restrictions, Future(true) otherwise.
    */
  private def removeCollaboratorFromOneFolder(
    folder: FolderRecord,
    policy: Option[SharingPolicyRecord],
    loggedInAs: String,
    collaborator: String
  )(implicit folderService: FolderService): Future[Boolean] = {
    if (isFolderAdmin(loggedInAs, folder, policy))
      folderService.removeCollaborator(folder.getId, collaborator)
    else
      Future.successful(false)
  }

  /** Does the actual work of removing a collaborator from 
    * a single DocumentRecord, if permissions allow.
    * 
    * Returns Future(false) when the collaborator was not removed
    * due to access restrictions, Future(true) otherwise.
    */
  private def removeCollaboratorFromOneDocument(
    document: DocumentRecord,
    policy: Option[SharingPolicyRecord],
    loggedInAs: String,
    collaborator: String
  )(implicit
      documentService: DocumentService,
      ctx: ExecutionContext
  ): Future[Boolean] = {
    if (isDocumentAdmin(loggedInAs, document, policy))
      documentService.removeDocumentCollaborator(document.getId, collaborator)
    else 
      Future.successful(false)
  }

  /** Removes a collaborator from all folders in the list.
    * 
    * This method is NOT recursive. Sub-folders will not be affected. 
    * Returns Future(false) if there was any folder where the collaborator
    * could not be removed due to access restrictions, Future(true)
    * otherwise.
    */
  private def removeCollaboratorFromFolderList(
    folders: Seq[(FolderRecord, Option[SharingPolicyRecord])],
    loggedInAs: String,
    collaborator: String
  )(implicit 
      folderService: FolderService,
      ctx: ExecutionContext
  ): Future[Boolean] = Future.sequence {
    folders.map(t => removeCollaboratorFromOneFolder(t._1, t._2, loggedInAs, collaborator))      
  } map { !_.exists(_ == false) }

  /** Removes a collaborator from all documents in the list.
    * 
    * Returns Future(false) if there was any document where the
    * collaborator could not be removed due to access restrictions, 
    * Future(true) otherwise.
    */
  private def removeCollaboratorFromDocumentList(
    documents: Seq[(DocumentRecord, Option[SharingPolicyRecord])],
    loggedInAs: String,
    collaborator: String
  )(implicit
      documentService: DocumentService,
      ctx: ExecutionContext
  ): Future[Boolean] = Future.sequence {
    documents.map(t => removeCollaboratorFromOneDocument(t._1, t._2, loggedInAs, collaborator))
  } map { !_.exists(_ == false) }

  /** Adds a collaborator to the given folder, the documents inside
    * it, and recursively all sub-folders and their documents. 
    *
    * Returns Future(false) if there was any folder or document where the
    * setting could not be applied due to access restrictions, Future(true)
    * otherwise.
    */
  def removeCollaboratorRecursive(
    folderId: UUID,
    loggedInAs: String,
    collaborator: String
  )(implicit
      folderService: FolderService,
      documentService: DocumentService,
      ctx: ExecutionContext
  ): Future[Boolean] = {
    val accessibleItems = listAccessibleItemsRecursive(folderId, loggedInAs)
    accessibleItems.flatMap { items =>
      val fFolderSuccess = removeCollaboratorFromFolderList(items.folders, loggedInAs, collaborator)
      val fDocumentSuccess = removeCollaboratorFromDocumentList(items.documents, loggedInAs, collaborator)
      for {
        folderSuccess <- fFolderSuccess
        documentSuccess <- fDocumentSuccess
      } yield folderSuccess && documentSuccess
    }
  }
  
}
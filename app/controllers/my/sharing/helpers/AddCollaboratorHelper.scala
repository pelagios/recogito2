package controllers.my.sharing.helpers

import AccessibleItems.Utils._
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import services.SharingLevel
import services.SharingLevel.Utils._
import services.document.DocumentService
import services.folder.FolderService
import services.generated.tables.records.{DocumentRecord, FolderRecord, SharingPolicyRecord}

trait AddCollaboratorHelper {

  /** Does the actual work of adding a collaborator to 
    * a single FolderRecord, if permissions allow.
    * 
    * Returns Future(false) when the collaborator was not added
    * due to access restrictions, Future(true) otherwise.
    */
  private def addCollaboratorToOneFolder(
    folder: FolderRecord,
    policy: Option[SharingPolicyRecord],
    loggedInAs: String,
    collaborator: String,
    level: SharingLevel
  )(implicit folderService: FolderService): Future[Boolean] = {
    if (isFolderAdmin(loggedInAs, folder, policy))
      folderService.addCollaborator(folder.getId, loggedInAs, collaborator, level)
    else
      Future.successful(false)
  }

  /** Does the actual work of adding a collaborator to 
    * a single DocumentRecord, if permissions allow.
    * 
    * Returns Future(false) when the collaborator was not added
    * due to access restrictions, Future(true) otherwise.
    */
  private def addCollaboratorToOneDocument(
    document: DocumentRecord,
    policy: Option[SharingPolicyRecord],
    loggedInAs: String,
    collaborator: String,
    level: SharingLevel
  )(implicit
      documentService: DocumentService,
      ctx: ExecutionContext
  ): Future[Boolean] = {
    if (isDocumentAdmin(loggedInAs, document, policy))
      documentService
        .addDocumentCollaborator(document.getId, loggedInAs, collaborator, level)
        .map(_ => true)
    else
      Future.successful(false)
  }

  /** Adds a collaborator to all folders in the list.
    * 
    * This method is NOT recursive. Sub-folders will not be affected. 
    * Returns Future(false) if there was any folder where the collaborator
    * could not be added due to access restrictions, Future(true)
    * otherwise.
    */
  private def addCollaboratorToFolderList(
    folders: Seq[(FolderRecord, Option[SharingPolicyRecord])],
    loggedInAs: String,
    collaborator: String,
    level: SharingLevel
  )(implicit 
      folderService: FolderService,
      ctx: ExecutionContext
  ): Future[Boolean] = Future.sequence {
    folders.map(t => addCollaboratorToOneFolder(t._1, t._2, loggedInAs, collaborator, level))      
  } map { !_.exists(_ == false) }

  /** Adss a collaborator to all documents in the list.
    * 
    * Returns Future(false) if there was any document where the
    * setting could not be applied due to access restrictions, 
    * Future(true) otherwise.
    */
  private def addCollaboratorToDocumentList(
    documents: Seq[(DocumentRecord, Option[SharingPolicyRecord])],
    loggedInAs: String,
    collaborator: String,
    level: SharingLevel
  )(implicit
      documentService: DocumentService,
      ctx: ExecutionContext
  ): Future[Boolean] = Future.sequence {
    documents.map(t => addCollaboratorToOneDocument(t._1, t._2, loggedInAs, collaborator, level))
  } map { !_.exists(_ == false) }

  /** Adds a collaborator to the given folder, the documents inside
    * it, and recursively all sub-folders and their documents. 
    *
    * Returns Future(false) if there was any folder or document where the
    * setting could not be applied due to access restrictions, Future(true)
    * otherwise.
    */
  def addCollaboratorRecursive(
    folderId: UUID,
    loggedInAs: String,
    collaborator: String,
    level: SharingLevel
  )(implicit
      folderService: FolderService,
      documentService: DocumentService,
      ctx: ExecutionContext
  ): Future[Boolean] = {

    val accessibleItems = listAccessibleItemsRecursive(folderId, loggedInAs)
    accessibleItems.flatMap { items =>
      val fFolderSuccess = addCollaboratorToFolderList(items.folders, loggedInAs, collaborator, level)
      val fDocumentSuccess = addCollaboratorToDocumentList(items.documents, loggedInAs, collaborator, level)
      for {
        folderSuccess <- fFolderSuccess
        documentSuccess <- fDocumentSuccess
      } yield folderSuccess && documentSuccess
    }
  }

}
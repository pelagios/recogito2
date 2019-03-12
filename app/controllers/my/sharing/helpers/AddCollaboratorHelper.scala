package controllers.my.sharing.helpers

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import services.SharingLevel
import services.SharingLevel.Utils._
import services.folder.FolderService
import services.generated.tables.records.{FolderRecord, SharingPolicyRecord}

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

  /** TODO **/
  private def addCollaboratorToOneDocument = ???

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

  /** TODO **/
  private def addCollaboratorToDocumentList = ???

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
      folderService: FolderService
  ): Future[Boolean] = {

    // TODO placeholder - this is just the folder. Not recursive, no nested documents
    folderService.addCollaborator(
      folderId, loggedInAs, collaborator, level
    )

  }

}
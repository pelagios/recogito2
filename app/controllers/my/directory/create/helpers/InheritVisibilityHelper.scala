package controllers.my.directory.create.helpers

import scala.concurrent.{ExecutionContext, Future}
import services.PublicAccess
import services.document.DocumentService
import services.folder.FolderService
import services.generated.tables.records.{DocumentRecord, FolderRecord}

trait InheritVisibilityHelper {

  def inheritVisibility(
    folder: FolderRecord,
    loggedInAs: String
  )(implicit 
      folderService: FolderService,
      ctx: ExecutionContext
  ): Future[Boolean] = Option(folder.getParent) match {

    case Some(parentId) => 
      folderService.getFolder(parentId, loggedInAs).flatMap { _ match { 
        case Some((parent, _)) =>
          val parentVisibility =
            PublicAccess.Visibility.withName(parent.getPublicVisibility)
          
          val parentAccessLevel = 
            Option(parent.getPublicAccessLevel).flatMap { str => 
              PublicAccess.AccessLevel.withName(str)
            }

          folderService.updateVisibilitySettings(folder.getId, parentVisibility, parentAccessLevel)

        case None =>
          // Can never happen - would be a foreign key violation
          throw new RuntimeException(s"Folder ${folder.getId} has parent ${parentId}, but parent not in DB")
      }}

    case None =>
      Future.successful(true)
  }   

  def inheritVisibility(
    document: DocumentRecord
  )(implicit 
      documentService: DocumentService,
      folderService: FolderService,
      ctx: ExecutionContext
  ): Future[Boolean] = {
    folderService.getContainingFolder(document.getId).flatMap { _ match {
      case Some(folder) =>
        val visibility = PublicAccess.Visibility.withName(folder.getPublicVisibility)
        val accessLevel = Option(folder.getPublicAccessLevel).flatMap { level => 
          PublicAccess.AccessLevel.withName(level) }

        documentService.setPublicAccessOptions(document.getId, visibility, accessLevel)

      case None =>
        // No parent folder
        Future.successful(true)
    }}
  }

}
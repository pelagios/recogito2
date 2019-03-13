package controllers.my.directory.create.helpers

import scala.concurrent.{ExecutionContext, Future}
import services.PublicAccess
import services.folder.FolderService
import services.generated.tables.records.FolderRecord

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
  
}
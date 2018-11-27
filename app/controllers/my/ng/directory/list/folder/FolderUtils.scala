package controllers.my.ng.directory.list.folder

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.Security
import controllers.my.ng.directory.list.DirectoryController
import java.util.UUID
import play.api.mvc.AnyContent
import scala.concurrent.Future
import services.folder.Breadcrumb

trait FolderUtils { self: DirectoryController =>

  protected def getBreadcrumbs(folderId: Option[UUID]) = folderId match {
    case Some(id) => folders.getBreadcrumbs(id)
    case None => Future.successful(Seq.empty[Breadcrumb])
  }

  protected def getReadme(maybeFolder: Option[UUID])(implicit request: SecuredRequest[Security.Env, AnyContent]) = maybeFolder match {
    case Some(folderId) => 
      folders.getFolder(folderId).map(_.map(_.getReadme))
    
    case None =>
      Future.successful(None)
  }
  
}
package controllers.my.ng.directory.delete

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import services.folder.FolderService
import services.user.UserService

@Singleton
class FolderActionsController @Inject() (
  val components: ControllerComponents,
  val folders: FolderService,
  val silhouette: Silhouette[Security.Env],
  val users: UserService,
  val config: Configuration,
  implicit val ctx: ExecutionContext
) extends BaseController(components, config, users)
    with HasPrettyPrintJSON {

  def deleteFolder(id: UUID) = silhouette.SecuredAction.async { implicit request =>
    folders.getFolder(id).flatMap { _ match {
      case Some(folder) =>
        val isOwner = folder.getOwner == request.identity.username
        if (isOwner) {
          folders.deleteFolder(folder.getId).map { success => 
            if (success) Ok
            else InternalServerError
          }
        } else {
          Future.successful(Forbidden)
        }

      case None => Future.successful(NotFound)
    }}
  }

  def bulkDeleteFolders() =
    silhouette.SecuredAction.async { implicit request => ??? }

  def unshareFolder(id: UUID) =
    silhouette.SecuredAction.async { implicit request => ??? }

  def bulkUnshareFolders() =
    silhouette.SecuredAction.async { implicit request => ??? }

  def deleteReadme(folderId: UUID) =
    silhouette.SecuredAction.async { implicit request => 
      val f = Option(folderId) match {
        case Some(id) => folders.deleteReadme(folderId)
        case None => users.deleteReadme(request.identity.username)
      }

      f.map { success => 
        if (success) Ok else InternalServerError
      }
    }

}
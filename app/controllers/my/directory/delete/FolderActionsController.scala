package controllers.my.directory.delete

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import services.SharingLevel
import services.folder.FolderService
import services.generated.tables.records.{FolderRecord, SharingPolicyRecord}
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

  /** Shorthand access check.
    * 
    * Returns true if the user is either owner or admin on the folder.
    */
  private def isAllowed(username: String, folder: FolderRecord, policy: Option[SharingPolicyRecord]) = {
    folder.getOwner == username || 
      policy.map { p => 
        p.getSharedWith == username && p.getAccessLevel == SharingLevel.ADMIN.toString
      }.getOrElse(false)
  }

  def deleteFolder(id: UUID) = silhouette.SecuredAction.async { implicit request =>
    folders.getFolder(id, request.identity.username).flatMap { _ match {
      case Some((folder, policy)) =>
        if (isAllowed(request.identity.username, folder, policy)) {
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
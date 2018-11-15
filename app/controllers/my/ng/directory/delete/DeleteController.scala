package controllers.my.ng.directory.delete

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import services.user.UserService

@Singleton
class DeleteController @Inject() (
  val components: ControllerComponents,
  val silhouette: Silhouette[Security.Env],
  val users: UserService,
  val config: Configuration,
  implicit val ctx: ExecutionContext
) extends BaseController(components, config, users)
    with HasPrettyPrintJSON {

  def deleteDocument(id: String) =
    silhouette.SecuredAction.async { implicit request => ??? }

  def bulkDeleteDocuments() =
    silhouette.SecuredAction.async { implicit request => ??? }

  def deleteFolder(id: UUID) =
    silhouette.SecuredAction.async { implicit request => ??? }

  def bulkDeleteFolders() =
    silhouette.SecuredAction.async { implicit request => ??? }

}
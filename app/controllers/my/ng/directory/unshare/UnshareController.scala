package controllers.my.ng.directory.unshare

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import services.user.UserService

@Singleton
class UnshareController @Inject() (
  val components: ControllerComponents,
  val silhouette: Silhouette[Security.Env],
  val users: UserService,
  val config: Configuration,
  implicit val ctx: ExecutionContext
) extends BaseController(components, config, users)
    with HasPrettyPrintJSON {

  def unshareDocument(id: String) =
    silhouette.SecuredAction.async { implicit request => ??? }

  def bulkUnshareDocuments() =
    silhouette.SecuredAction.async { implicit request => ??? }

  def unshareFolder(id: UUID) =
    silhouette.SecuredAction.async { implicit request => ??? }

  def bulkUnshareFolders() =
    silhouette.SecuredAction.async { implicit request => ??? }

}
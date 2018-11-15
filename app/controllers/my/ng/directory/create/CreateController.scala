package controllers.my.ng.directory.create

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import services.folder.FolderService
import services.user.UserService

@Singleton
class CreateController @Inject() (
  val components: ControllerComponents,
  val folders: FolderService,
  val silhouette: Silhouette[Security.Env],
  val users: UserService,
  val config: Configuration,
  implicit val ctx: ExecutionContext
) extends BaseController(components, config, users)
    with HasPrettyPrintJSON {

  def createFolder() = silhouette.SecuredAction.async { implicit request => 
    request.body.asJson match {
      case None => Future.successful(BadRequest)

      case Some(json) =>
        (json \ "title").asOpt[String] match {
          case None => Future.successful(BadRequest)

          case Some(title) =>
            val parent = (json \ "parent").asOpt[UUID]
            folders.createFolder(request.identity.username, title, parent).map { folder => 
              jsonOk(Json.obj("id" -> folder.getId))
            }
        }
    }
  }

}
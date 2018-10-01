package controllers.my.ng

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import services.document.DocumentService
import services.user.UserService

@Singleton
class WorkspaceAPIController @Inject() (
    val components: ControllerComponents,
    val documents: DocumentService,
    val silhouette: Silhouette[Security.Env],
    val users: UserService,
    val config: Configuration,
    implicit val ctx: ExecutionContext
  ) extends BaseController(components, config, users)
      with HasPrettyPrintJSON {
    
  def my = silhouette.SecuredAction.async { implicit request =>
    documents.findByOwner(request.identity.username).map { documents =>
      // DocumentRecord JSON serialization
      import services.document.DocumentService._
      jsonOk(Json.toJson(documents))
    }
  }

}
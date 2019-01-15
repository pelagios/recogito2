package controllers.api

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import services.announcement.AnnouncementService
import services.document.DocumentService
import services.user.UserService

@Singleton
class AnnouncementAPIController @Inject() (
  val announcements: AnnouncementService,
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val silhouette: Silhouette[Security.Env],
  val users: UserService,
  implicit val ctx: ExecutionContext
) extends BaseAuthController(components, config, documents, users) with HasPrettyPrintJSON {

  def myLatest = silhouette.SecuredAction.async { implicit request =>
    announcements.findLatestUnread(request.identity.username).map { _ match {
      case Some(message) => 
        Ok(Json.obj(
          "id" -> message.getId,
          "content" -> message.getContent
        ))

      case None => NotFound
    }}
  }

  def confirm(id: UUID) = silhouette.SecuredAction.async { implicit request =>
    announcements.confirm(id, request.identity.username, "OK").map { success => 
      if (success) Ok else BadRequest
    }
  }

}
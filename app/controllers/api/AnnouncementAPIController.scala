package controllers.api

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, HasPrettyPrintJSON, Security}
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

  /*
  implicit val announcementWrites: Writes[ServiceAnnouncementRecord] = (
    (JsPath \ "identifier").write[String] and
    (JsPath \ "name").write[String] and
    (JsPath \ "languages").write[Seq[String]] and
    (JsPath \ "organization").write[String] and
    (JsPath \ "description").write[String] and 
    (JsPath \ "version").write[String]
  )(p => (
     p.getClass.getName,
     p.getName,
     p.getSupportedLanguages.asScala,
     p.getOrganization,
     p.getDescription,
     p.getVersion
  ))
  */

  /*
  id UUID PRIMARY KEY,
  for_user TEXT NOT NULL REFERENCES "user"(username),
  content TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  viewed_at TIMESTAMP WITH TIME ZONE,
  response TEXT
  */

  def myLatest = silhouette.SecuredAction.async { implicit request =>
    announcements.findLatestUnread(request.identity.username).map { _ match {
      case Some(message) => 
        Ok(Json.obj("content" -> message.getContent))

      case None => NotFound
    }}
  }

  def reply = silhouette.SecuredAction.async { implicit request =>
    null
  }

}
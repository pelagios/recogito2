package controllers.admin.users

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import services.{HasDate, SortOrder}
import services.annotation.AnnotationService
import services.announcement.AnnouncementService
import services.contribution.ContributionService
import services.document.DocumentService
import services.upload.UploadService
import services.user.Roles._
import services.user.UserService
import services.generated.tables.records.UserRecord
import org.joda.time.DateTime
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.{Json, Writes, JsPath}
import play.api.libs.functional.syntax._
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}
import org.joda.time.format.DateTimeFormat
import java.sql.Timestamp
import controllers.HasAccountRemoval

@Singleton
class UserAdminController @Inject() (
  val components: ControllerComponents, 
  val config: Configuration,
  val silhouette: Silhouette[Security.Env],
  implicit val annotations: AnnotationService,
  implicit val announcements: AnnouncementService,
  implicit val contributions: ContributionService,
  implicit val ctx: ExecutionContext,
  implicit val documents: DocumentService,
  implicit val uploads: UploadService,
  implicit val users: UserService,
  implicit val webJarsUtil: WebJarsUtil
) extends BaseAuthController(components, config, documents, users) with HasPrettyPrintJSON with HasDate with HasAccountRemoval {
  
  private val fmt = DateTimeFormat.forPattern("yyyy-MM-dd")

  implicit val userRecordWrites: Writes[UserRecord] = (
    (JsPath \ "username").write[String] and
    (JsPath \ "email").write[String] and
    (JsPath \ "member_since").write[DateTime] and
    (JsPath \ "real_name").writeNullable[String] and
    (JsPath \ "bio").writeNullable[String] and
    (JsPath \ "website").writeNullable[String] and
    (JsPath \ "quota").write[Int] and
    (JsPath \ "last_login").write[DateTime]
  )(user => (
    user.getUsername,
    users.decryptEmail(user.getEmail),
    new DateTime(user.getMemberSince.getTime),
    Option(user.getRealName),
    Option(user.getBio),
    Option(user.getWebsite),
    user.getQuotaMb,
    new DateTime(user.getLastLogin.getTime)
  ))

  implicit val userWithAdminStatusWrites: Writes[(UserRecord, Boolean)] = (
    (JsPath).write[UserRecord] and
    (JsPath \ "is_admin").write[Boolean]
  )(t => (t._1, t._2))

  def index = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request =>
    Ok(views.html.admin.users.index())
  }
  
  def listUsers(offset: Int, size: Int, sortBy: Option[String], sortOrder: Option[String]) = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    users.listUsers(offset, size, sortBy, sortOrder.flatMap(o => SortOrder.fromString(o))).map { users =>
      jsonOk(Json.toJson(users))
    }
  }
  
  def listIdleUsers(date: String, offset: Int, size: Int) = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    Try(fmt.parseDateTime(date)) match {
      case Success(date) =>
        users.listIdleUsers(new Timestamp(date.getMillis), offset, size).map { users =>
          jsonOk(Json.toJson(users))
        }
        
      case Failure(t) =>
        Future.successful(BadRequest(s"Invalid date: ${date}")) 
    }
  }

  def updateSettings(username: String) = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request => 
    request.body.asJson match {
      case Some(json) =>
        val quota = (json \ "quota").as[Int]
        val isAdmin = (json \ "is_admin").asOpt[Boolean].getOrElse(false)

        // TODO implement
        Future.successful(Ok)

      case None => 
        Future.successful(BadRequest) // Cannot happen via UI
    }
  }
  
  def deleteAccount(username: String) = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    deleteUserAccount(username).map(_ => Ok)
  }

}

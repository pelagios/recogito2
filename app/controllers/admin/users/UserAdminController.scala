package controllers.admin.users

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import services.{HasDate, SortOrder}
import services.document.DocumentService
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

@Singleton
class UserAdminController @Inject() (
  val components: ControllerComponents, 
  val config: Configuration,
  val documents: DocumentService,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext,
  implicit val webJarsUtil: WebJarsUtil
) extends BaseAuthController(components, config, documents, users) with HasPrettyPrintJSON with HasDate {
  
  private val fmt = DateTimeFormat.forPattern("yyyy-MM-dd")
  
  implicit val userRecordWrites: Writes[UserRecord] = (
    (JsPath \ "username").write[String] and
    (JsPath \ "email").write[String] and
    (JsPath \ "member_since").write[DateTime] and
    (JsPath \ "real_name").writeNullable[String] and
    (JsPath \ "quota").write[Int] and
    (JsPath \ "last_login").write[DateTime]
  )(user => (
    user.getUsername,
    users.decryptEmail(user.getEmail),
    new DateTime(user.getMemberSince.getTime),
    Option(user.getRealName),
    user.getQuotaMb,
    new DateTime(user.getLastLogin.getTime)
  ))

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

}

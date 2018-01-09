package controllers.admin.users

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import models.{HasDate, SortOrder}
import models.document.DocumentService
import models.user.Roles._
import models.user.UserService
import org.joda.time.DateTime
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext

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

  def index = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request =>
    Ok(views.html.admin.users.index())
  }
  
  def listUsers(offset: Int, size: Int, sortBy: Option[String], sortOrder: Option[String]) = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    users.listUsers(offset, size, sortBy, sortOrder.flatMap(o => SortOrder.fromString(o))).map { userList =>
      jsonOk(Json.toJson(userList.map { user =>
        Json.obj(
          "username" -> user.getUsername,
          "email" -> users.decrypt(user.getEmail),
          "member_since" -> formatDate(new DateTime(user.getMemberSince.getTime)),
          "real_name" -> user.getRealName,
          "quota" -> user.getQuotaMb.toInt
        )
      }))
    }
  }

}

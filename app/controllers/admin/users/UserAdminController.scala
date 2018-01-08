package controllers.admin.users

import controllers.{ BaseAuthController, HasPrettyPrintJSON }
import javax.inject.{ Inject, Singleton }
import models.{ HasDate, SortOrder }
import models.document.DocumentService
import models.user.Roles._
import models.user.UserService
import org.joda.time.DateTime
import org.webjars.play.WebJarsUtil
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import play.api.Configuration
import play.api.libs.json.Json

@Singleton
class UserAdminController @Inject() (
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService,
    implicit val ctx: ExecutionContext,
    implicit val webJarsUtil: WebJarsUtil
  ) extends BaseAuthController(config, documents, users) with HasPrettyPrintJSON with HasDate {

  def index = StackAction(AuthorityKey -> Admin) { implicit request =>
    Ok(views.html.admin.users.index())
  }
  
  def listUsers(offset: Int, size: Int, sortBy: Option[String], sortOrder: Option[String]) = AsyncStack(AuthorityKey -> Admin) { implicit request =>
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

package controllers.api

import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import controllers.{ BaseController, HasPrettyPrintJSON }
import models.SortOrder
import models.user.UserService
import models.user.Roles.Admin
import models.generated.tables.records.UserRecord
import play.api.Configuration
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import models.HasDate
import org.joda.time.DateTime
import play.api.libs.json.JsNumber

@Singleton
class UserAPIController @Inject() (
    val config: Configuration,
    val users: UserService,
    implicit val ctx: ExecutionContext
  ) extends BaseController(config, users) with AuthElement with HasPrettyPrintJSON with HasDate {

  /** TODO redirects to login for unauthorized users - as a JSON method, should send FORBIDDEN **/
  def listUsers(offset: Int, size: Int, sortBy: Option[String], sortOrder: Option[String]) = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    users.listUsers(offset, size, sortBy, sortOrder.flatMap(o => SortOrder.fromString(o))).map { userList =>
      jsonOk(Json.toJson(userList.map { user =>
        Json.obj(
          "username" -> user.getUsername,
          "email" -> users.decrypt(user.getEmail),
          "member_since" -> this.formatDate(new DateTime(user.getMemberSince.getTime)),
          "real_name" -> user.getRealName,
          "quota" -> user.getQuotaMb.toInt
        )
      }))
    }
  }

}

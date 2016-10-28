package controllers.api

import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import controllers.{ BaseController, HasPrettyPrintJSON }
import models.user.UserService
import models.user.Roles.Admin
import models.generated.tables.records.UserRecord
import play.api.Configuration
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import models.HasDate
import org.joda.time.DateTime
import play.api.libs.json.JsNumber

class UserAPIController @Inject() (
    val config: Configuration,
    val users: UserService,
    implicit val ctx: ExecutionContext
  ) extends BaseController(config, users) with AuthElement with HasPrettyPrintJSON with HasDate {
  
  def listUsers(offset: Int, size: Int) = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    users.listUsers(offset, size).map { userList =>
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
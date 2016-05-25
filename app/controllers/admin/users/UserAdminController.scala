package controllers.admin.users

import controllers.BaseController
import javax.inject.Inject
import models.user.Roles._
import models.user.UserService
import play.api.cache.CacheApi
import scala.concurrent.ExecutionContext
import storage.DB

class UserAdminController @Inject() (implicit val cache: CacheApi, val db: DB, ec: ExecutionContext) extends BaseController {
  
  def index = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    UserService.listUsers().map { users => 
      Ok(views.html.admin.users(users))   
    }
  }
  
}
package controllers.admin

import controllers.BaseController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class AdminController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {
  
  /** TODO placeholder **/
  def backup = StackAction(AuthorityKey -> Admin) { implicit request =>
    Ok(views.html.admin.backup())
  }
      
  /** TODO placeholder **/
  def users = StackAction(AuthorityKey -> Admin) { implicit request =>
    Ok(views.html.admin.users())
  }
        
  def index = StackAction(AuthorityKey -> Admin) { implicit request =>
    Redirect(controllers.admin.gazetteer.routes.GazetteerAdminController.index)
  }
  
}
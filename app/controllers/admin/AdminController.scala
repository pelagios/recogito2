package controllers.admin

import controllers.BaseAuthController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class AdminController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseAuthController {
  
  /** TODO placeholder **/
  def backup = StackAction(AuthorityKey -> Admin) { implicit request =>
    Ok(views.html.admin.backup())
  }
        
  /** TODO placeholder **/
  def index = StackAction(AuthorityKey -> Admin) { implicit request =>
    Redirect(controllers.admin.gazetteers.routes.GazetteerAdminController.index)
  }
  
}
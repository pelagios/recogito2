package controllers.admin

import controllers.BaseAuthController
import javax.inject.Inject
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import play.api.Configuration

class AdminController @Inject() (
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService
  ) extends BaseAuthController(config, documents, users) {
        
  /** TODO placeholder **/
  def index = StackAction(AuthorityKey -> Admin) { implicit request =>
    Redirect(controllers.admin.gazetteers.routes.GazetteerAdminController.index())
  }
  
}
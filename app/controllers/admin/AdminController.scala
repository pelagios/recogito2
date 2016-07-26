package controllers.admin

import controllers.{ BaseAuthController, ControllerContext }
import javax.inject.Inject
import models.user.Roles._

class AdminController @Inject() (implicit val ctx: ControllerContext) extends BaseAuthController {
        
  /** TODO placeholder **/
  def index = StackAction(AuthorityKey -> Admin) { implicit request =>
    Redirect(controllers.admin.gazetteers.routes.GazetteerAdminController.index)
  }
  
}
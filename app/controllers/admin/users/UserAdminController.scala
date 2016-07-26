package controllers.admin.users

import controllers.{ BaseAuthController, ControllerContext }
import javax.inject.Inject
import models.document.DocumentService
import models.user.Roles._
import models.user.UserService
import scala.concurrent.Future

class UserAdminController @Inject() (implicit val ctx: ControllerContext) extends BaseAuthController {

  def index = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    UserService.listUsers(0, 500).map(users => 
      Ok(views.html.admin.users.index(users)))
  }

  def showDetails(username: String) = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    UserService.findByUsername(username).flatMap(_ match {

      case Some(user) =>
        DocumentService.findByOwner(username).map(documents =>
          Ok(views.html.admin.users.details(user, documents)))

      case None => Future.successful(NotFoundPage)

    })
  }

}

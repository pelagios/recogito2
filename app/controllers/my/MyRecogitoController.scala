package controllers.my

import controllers.{ AbstractController, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.user.Roles._
import models.user.UserService
import models.content.DocumentService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import storage.DB

class MyRecogitoController @Inject() (implicit val db: DB) extends AbstractController with AuthElement with Security {

  def my = StackAction(AuthorityKey -> Normal) { implicit request =>
    Redirect(routes.MyRecogitoController.index(loggedIn.getUsername))
  }
  
  def index(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    val loggedInUser = loggedIn.getUsername
    if (loggedInUser == usernameInPath) {
      // Personal space
      DocumentService.findByUser(loggedInUser).map(documents => {
        Ok(views.html.my.index(loggedIn, UserService.getUsedDiskspaceKB(loggedInUser), documents))
      })
    } else {
      // TODO show public profile instead
      Future.successful(Forbidden)
    }
  }

}

package controllers.my

import controllers.BaseController
import javax.inject.Inject
import models.user.Roles._
import models.user.UserService
import models.document.DocumentService
import play.api.Play
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import storage.DB

class MyRecogitoController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {
  
  // TODO this may depend on user in the future
  private lazy val QUOTA = Play.current.configuration.getInt("recogito.upload.quota").getOrElse(200)

  def my = StackAction(AuthorityKey -> Normal) { implicit request =>
    Redirect(routes.MyRecogitoController.index(loggedIn.user.getUsername))
  }
  
  def index(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    val loggedInUser = loggedIn.user.getUsername
    if (loggedInUser == usernameInPath) {
      // Personal space
      DocumentService.findByOwner(loggedInUser).map(documents => {
        Ok(views.html.my.index(loggedIn.user, UserService.getUsedDiskspaceKB(loggedInUser), QUOTA, documents))
      })
    } else {
      // TODO show public profile instead
      Future.successful(Forbidden)
    }
  }

}

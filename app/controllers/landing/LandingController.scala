package controllers.landing

import controllers.{ HasConfig, HasUserService, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.user.UserService
import play.api.Configuration
import play.api.mvc.Controller

class LandingController @Inject() (
    val config: Configuration,
    val users: UserService)
  extends Controller with HasConfig with HasUserService with OptionalAuthElement with Security {

  def index = StackAction { implicit request =>
    loggedIn match {
      case Some(user) =>
        Redirect(controllers.my.routes.MyRecogitoController.index(user.user.getUsername, None, None, None))

      case None =>
        Ok(views.html.landing.index())
    }
  }

}

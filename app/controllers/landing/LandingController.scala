package controllers.landing

import controllers.{ HasConfig, HasUserService, HasVisitLogging, Security, WebJarAssets }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models.user.UserService
import models.visit.VisitService
import play.api.Configuration
import play.api.mvc.Controller

class LandingController @Inject() (
    val config: Configuration,
    val users: UserService,
    implicit val visits: VisitService,
    implicit val webjars: WebJarAssets
) extends Controller with HasConfig with HasUserService with OptionalAuthElement with Security with HasVisitLogging {

  def index = StackAction { implicit request =>
    loggedIn match {
      case Some(user) =>
        Redirect(controllers.my.routes.MyRecogitoController.index(user.user.getUsername, None, None, None, None))

      case None =>
        logPageView()
        Ok(views.html.landing.index())
    }
  }

}

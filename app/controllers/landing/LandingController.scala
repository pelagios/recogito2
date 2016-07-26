package controllers.landing

import controllers.{ ControllerContext, HasContext, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import play.api.mvc.Controller
  
class LandingController @Inject() (implicit val ctx: ControllerContext) 
  extends Controller with HasContext with OptionalAuthElement with Security {

  def index = StackAction { implicit request =>
    loggedIn match {
      case Some(user) =>
        Redirect(controllers.my.routes.MyRecogitoController.index(user.user.getUsername, None))
        
      case None =>
        Ok(views.html.landing.index())
    }
  }

}

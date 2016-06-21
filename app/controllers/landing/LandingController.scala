package controllers.landing

import controllers.{ HasCache, HasDatabase, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.OptionalAuthElement
import play.api.cache.CacheApi
import play.api.mvc.Controller
import storage.DB
  
class LandingController @Inject() (implicit val cache: CacheApi, val db: DB) 
  extends Controller with HasCache with HasDatabase with OptionalAuthElement with Security {

  def index = StackAction { implicit request =>
    loggedIn match {
      case Some(user) =>
        Redirect(controllers.my.routes.MyRecogitoController.index(user.user.getUsername, None))
        
      case None =>
        Ok(views.html.landing.index())
    }
  }

}

package controllers.landing

import controllers.{ AbstractController, Security }
import database.DB
import javax.inject.Inject
import jp.t2v.lab.play2.auth.Logout
import play.api.mvc.Action
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class LogoutController @Inject() (implicit val db: DB) extends AbstractController with Logout with Security {

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

}

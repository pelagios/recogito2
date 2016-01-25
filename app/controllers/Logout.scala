package controllers

import database.DB
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.{ Action, Controller }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.Form
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import jp.t2v.lab.play2.auth.LoginLogout
import scala.concurrent.Future


/** Just temporary container for everything while we're still hacking on the basics **/
class Logout @Inject() (implicit val db: DB) extends Controller with LoginLogout with HasDB with AuthConfigImpl {

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

}

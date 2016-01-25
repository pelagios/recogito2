package controllers

import database.DB
import javax.inject.Inject
import models.Users
import play.api.Logger
import play.api.mvc.{ Action, Controller }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.Form
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import jp.t2v.lab.play2.auth.LoginLogout
import scala.util.{ Success, Failure }
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._

case class LoginData(name: String, password: String)

/** Just temporary container for everything while we're still hacking on the basics **/
class Login @Inject() (implicit val db: DB) extends Controller with LoginLogout with HasDB with AuthConfigImpl {

  val loginForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )

  def showLoginForm = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def processLogin = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        Logger.info("Bad request!")
        BadRequest(views.html.login(formWithErrors))
      },

      loginData => {
        val isValid = Await.result(Users.validateUser(loginData.name,loginData.password), 10 seconds)
        if (isValid)
          Await.result(gotoLoginSucceeded(loginData.name), 10 seconds)
        else
          Redirect(routes.Login.showLoginForm()).flashing("message" -> "Invalid username or password")
      }
    )
  }

}

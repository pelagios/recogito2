package controllers

import database.DB
import javax.inject.Inject
import jp.t2v.lab.play2.auth.LoginLogout
import models.Users
import play.api.Logger
import play.api.mvc.{ Action, Controller }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.Form
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import scala.concurrent.Future

case class LoginData(username: String, password: String)

/** Just temporary container for everything while we're still hacking on the basics **/
class Login @Inject() (implicit val db: DB) extends Controller with LoginLogout with HasDB with AuthConfigImpl {

  private val MESSAGE = "message"
  
  private val INVALID_LOGIN = "Invalid Username or Password"
  
  val loginForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )

  def showLoginForm = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def processLogin = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        Logger.info("Bad request!")
        Future(BadRequest(views.html.login(formWithErrors)))
      },

      loginData => {
        Users.validateUser(loginData.username, loginData.password).flatMap(isValid => {
          if (isValid)
            gotoLoginSucceeded(loginData.username)
          else
            Future(Redirect(routes.Login.showLoginForm()).flashing(MESSAGE -> INVALID_LOGIN))       
        })
      }
    )
  }

}

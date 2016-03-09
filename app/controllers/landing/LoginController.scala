package controllers.landing

import controllers.{ AbstractController, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.Login
import models.user.UserService
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import scala.concurrent.Future
import storage.DB

case class LoginData(username: String, password: String)

class LoginController @Inject() (implicit val db: DB) extends AbstractController with Login with Security {

  private val MESSAGE = "message"

  private val INVALID_LOGIN = "Invalid Username or Password"

  val loginForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )

  def showLoginForm = Action { implicit request =>
    Ok(views.html.landing.login(loginForm))
  }

  def processLogin = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors =>
        Future(BadRequest(views.html.landing.login(formWithErrors))),

      loginData =>
        UserService.validateUser(loginData.username, loginData.password).flatMap(isValid => {
          if (isValid)
            gotoLoginSucceeded(loginData.username)
          else
            Future(Redirect(routes.LoginController.showLoginForm()).flashing(MESSAGE -> INVALID_LOGIN))
        })
    )
  }

}

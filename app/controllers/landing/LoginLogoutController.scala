package controllers.landing

import controllers.{ HasConfig, HasUserService, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.{ AuthElement, LoginLogout }
import models.document.DocumentService
import models.user.UserService
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, Controller }
import scala.concurrent.{ ExecutionContext, Future }

case class LoginData(username: String, password: String)

class LoginLogoutController @Inject() (
    val config: Configuration,
    val users: UserService,
    implicit val ctx: ExecutionContext,
    val messagesApi: MessagesApi
  ) extends Controller with AuthElement with HasConfig with HasUserService with Security with LoginLogout with I18nSupport {

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
        users.validateUser(loginData.username, loginData.password).flatMap(isValid => {
          if (isValid)
            gotoLoginSucceeded(loginData.username)
          else
            Future(Redirect(routes.LoginLogoutController.showLoginForm()).flashing(MESSAGE -> INVALID_LOGIN))
        })
    )
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

}

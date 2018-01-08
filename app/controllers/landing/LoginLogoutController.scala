package controllers.landing

import controllers.{ HasConfig, HasUserService }
import javax.inject.{ Inject, Singleton }
import models.document.DocumentService
import models.user.UserService
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, ControllerComponents }
import scala.concurrent.{ ExecutionContext, Future }
import play.api.mvc.AbstractController
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.LoginInfo

case class LoginData(usernameOrPassword: String, password: String)

@Singleton
class LoginLogoutController @Inject() (  
    val components: ControllerComponents,
    val config: Configuration,
    val users: UserService,
    val silhouette: Silhouette[DefaultEnv],
    implicit val ctx: ExecutionContext
    // val messagesApi: MessagesApi
  ) extends AbstractController(components) with HasConfig with HasUserService with I18nSupport {

  private val MESSAGE = "message"

  private val INVALID_LOGIN = "Invalid Username or Password"

  val loginForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )

  def showLoginForm(destination: Option[String]) = Action { implicit request =>
    destination match {
      case None => Ok(views.html.landing.login(loginForm))
      case Some(dest) => Ok(views.html.landing.login(loginForm)).withSession("access_uri" -> dest)
    }
  }

  def processLogin = silhouette.UnsecuredAction.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors =>
        Future(BadRequest(views.html.landing.login(formWithErrors))),

      loginData =>
        users.validateUser(loginData.usernameOrPassword, loginData.password).flatMap {
          case Some(validUser) =>
            silhouette.env.authenticatorService.create(LoginInfo("recogito", validUser.getUsername)).flatMap { authenticator =>
              silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
                silhouette.env.authenticatorService.embed(v, Ok)
              }
            }
                        
          case None => Future(Redirect(routes.LoginLogoutController.showLoginForm()).flashing(MESSAGE -> INVALID_LOGIN))
        }
    )
  }

  def logout = Action.async { implicit request =>
    Future.successful(Ok) // gotoLogoutSucceeded
  }

}

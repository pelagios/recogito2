package controllers.landing

import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import controllers.{HasConfig, HasUserService, Security}
import javax.inject.{Inject, Singleton}
import models.user.UserService
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{ ExecutionContext, Future }

case class LoginData(usernameOrPassword: String, password: String)

@Singleton
class LoginLogoutController @Inject() (  
    val components: ControllerComponents,
    val config: Configuration,
    val silhouette: Silhouette[Security.Env],
    val users: UserService,
    implicit val ctx: ExecutionContext
  ) extends AbstractController(components) with HasConfig with HasUserService with I18nSupport {

  private val MESSAGE = "message"

  private val INVALID_LOGIN = "Invalid Username or Password"
  
  private val auth = silhouette.env.authenticatorService

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

  def processLogin = silhouette.UserAwareAction.async { implicit request =>    
    loginForm.bindFromRequest.fold(
      formWithErrors =>
        Future(BadRequest(views.html.landing.login(formWithErrors))),

      loginData =>
        users.validateUser(loginData.usernameOrPassword, loginData.password).flatMap {
          case Some(validUser) =>
            auth.create(LoginInfo("recogito", validUser.getUsername))
              .flatMap(auth.init(_))
              .flatMap(auth.embed(_,  Redirect(routes.LandingController.index)))
                        
          case None => Future(Redirect(routes.LoginLogoutController.showLoginForm()).flashing(MESSAGE -> INVALID_LOGIN))
        }
    )
  }

  def logout = silhouette.SecuredAction.async { implicit request =>
    auth.discard(request.authenticator, Redirect(routes.LandingController.index))
  }

}

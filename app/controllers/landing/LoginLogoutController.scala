package controllers.landing

import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import controllers.{HasConfig, HasUserService, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import services.announcement.AnnouncementService
import services.user.UserService
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{ ExecutionContext, Future }

case class LoginData(usernameOrPassword: String, password: String)

@Singleton
class LoginLogoutController @Inject() (  
    val announcements: AnnouncementService,
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
            
            val destination = request.session.get("access_uri").getOrElse(routes.LandingController.index.toString)
            users.updateLastLogin(validUser.getUsername)
            
            val fAnnouncement = announcements.findLatestUnread(validUser.getUsername)           
            val fAuthentication = auth.create(LoginInfo(Security.PROVIDER_ID, validUser.getUsername))
              .flatMap(auth.init(_))
              
            val f = for {
              announcement <- fAnnouncement
              authentication <- fAuthentication
            } yield (announcement, authentication)
            
            f.flatMap {
              case (Some(announcement), authentication) =>
                auth.embed(authentication, 
                  Ok(views.html.landing.announcement(announcement.getId, announcement.getContent, destination))
                    .withSession(request.session - "access_uri"))
                
              case (None, authentication) =>               
                auth.embed(authentication,
                  Redirect(destination).withSession(request.session - "access_uri"))
            }
          
          case None => Future(Redirect(routes.LoginLogoutController.showLoginForm()).flashing(MESSAGE -> INVALID_LOGIN))
        }
    )
  }

  def confirmServiceAnnouncement(id: UUID, response: String, destination: Option[String]) = silhouette.SecuredAction.async { implicit request =>
    announcements.confirm(id, request.identity.username, response).map { success =>
      if (success) destination.map(Redirect(_)).getOrElse(Redirect(routes.LandingController.index))
      else InternalServerError
    }
  }

  def logout = silhouette.SecuredAction.async { implicit request =>
    auth.discard(request.authenticator, Redirect(routes.LandingController.index))
  }

}

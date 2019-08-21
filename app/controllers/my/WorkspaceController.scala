package controllers.my

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, Security, HasPrettyPrintJSON}
import javax.inject.{Inject, Singleton}
import services.contribution.{Contribution, ContributionService}
import services.user.{User, UserService}
import org.webjars.play.WebJarsUtil
import play.api.{Configuration, Environment}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, RequestHeader}
import services.document.DocumentService
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WorkspaceController @Inject() (
  val components: ControllerComponents,
  val contributions: ContributionService,
  val users: UserService,
  val config: Configuration,
  val silhouette: Silhouette[Security.Env],
  implicit val documents: DocumentService,
  implicit val ctx: ExecutionContext,
  implicit val env: Environment,
  implicit val webjars: WebJarsUtil
) extends BaseController(components, config, users) with I18nSupport with HasPrettyPrintJSON {

  /** A convenience '/my' route that redirects to the personal index **/
  def my = silhouette.UserAwareAction { implicit request =>
    request.identity match {
      case Some(userWithRoles) =>
        Redirect(routes.WorkspaceController.workspace(userWithRoles.username.toLowerCase))

      case None =>
        // Not logged in - go to log in and then come back here
        Redirect(controllers.landing.routes.LoginLogoutController.showLoginForm(None))
          .withSession("access_uri" -> routes.WorkspaceController.my.url)
    }
  }

  private def renderPublicProfile(usernameInPath: String, loggedInUser: Option[User])(implicit request: RequestHeader) = {
    users.findByUsernameIgnoreCase(usernameInPath).map { _ match {
      case Some(owner) => Ok(views.html.my.profile())
      case None => NotFoundPage
    }}
  }    

  /**  User workspace **/
  def workspace(usernameInPath: String) = silhouette.UserAwareAction.async { implicit request =>    
    // If the user is logged in & the name in the path == username it's the profile owner
    val isProfileOwner = request.identity match {
      case Some(userWithRoles) => userWithRoles.username.equalsIgnoreCase(usernameInPath)
      case None => false
    }
    
    if (isProfileOwner)
      Future.successful(Ok(views.html.my.workspace()))
    else
      renderPublicProfile(usernameInPath, request.identity)
  }

  def activityFeed(usernameInPath: String) = silhouette.UserAwareAction.async { implicit request =>
    val loggedInAs = request.identity.map(_.username)
    contributions.getUserActivityFeed(Seq(usernameInPath), loggedInAs).map { response => 
      jsonOk(Json.toJson(response))
    } 
  }
  
}

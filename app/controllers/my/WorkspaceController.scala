package controllers.my

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, Security}
import javax.inject.{Inject, Singleton}
import services.{Page, SortOrder}
import services.annotation.AnnotationService
import services.contribution.{Contribution, ContributionService}
import services.user.{User, UserService}
import services.document.DocumentService
import services.generated.tables.records.{DocumentRecord, SharingPolicyRecord}
import org.webjars.play.WebJarsUtil
import play.api.{Configuration, Environment}
import play.api.i18n.I18nSupport
import play.api.mvc.{ControllerComponents, Cookie, RequestHeader}
import scala.concurrent.{ExecutionContext, Future}
import storage.uploads.Uploads

@Singleton
class WorkspaceController @Inject() (
    val annotations: AnnotationService,
    val components: ControllerComponents,
    val contributions: ContributionService,
    val documents: DocumentService,
    val users: UserService,
    val uploads: Uploads,
    val config: Configuration,
    val silhouette: Silhouette[Security.Env],
    implicit val ctx: ExecutionContext,
    implicit val env: Environment,
    implicit val webjars: WebJarsUtil
  ) extends BaseController(components, config, users) with I18nSupport {

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

  private def renderPublicProfile(usernameInPath: String, loggedInUser: Option[User])(implicit request: RequestHeader) = {
    users.findByUsernameIgnoreCase(usernameInPath).map { _ match {
      case Some(owner) => Ok(views.html.my.profile())
      case None => NotFoundPage
    }}
  }    
  
}

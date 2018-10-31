package controllers.my.settings

import com.mohiva.play.silhouette.api.{Silhouette, LoginInfo}
import controllers.{HasUserService, HasConfig, Security}
import javax.inject.Inject
import services.announcement.AnnouncementService
import services.annotation.AnnotationService
import services.contribution.ContributionService
import services.user.Roles._
import services.user.UserService
import services.upload.UploadService
import services.document.DocumentService
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import controllers.HasAccountRemoval

case class AccountSettingsData(
  email  : String,
  name   : Option[String],
  bio    : Option[String],
  website: Option[String])

class AccountSettingsController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val silhouette: Silhouette[Security.Env],
  implicit val announcements: AnnouncementService,
  implicit val annotations: AnnotationService,
  implicit val contributions: ContributionService,
  implicit val ctx: ExecutionContext,
  implicit val documents: DocumentService,
  implicit val uploads: UploadService,
  implicit val users: UserService,
  implicit val webjars: WebJarsUtil
) extends AbstractController(components) with HasUserService with HasConfig with HasAccountRemoval with I18nSupport {

  val accountSettingsForm = Form(
    mapping(
      "email" -> email,
      "name" -> optional(text(maxLength=80)),
      "bio" -> optional(text(maxLength=256)),
      "website" -> optional(text(maxLength=256))
    )(AccountSettingsData.apply)(AccountSettingsData.unapply)
  )

  def index() = silhouette.SecuredAction { implicit request =>
    val u = request.identity
    
    val form = accountSettingsForm.fill(AccountSettingsData(
      users.decryptEmail(u.email),
      u.realName,
      u.bio,
      u.website))
    
    Ok(views.html.my.settings.account(form, u))
  }

  def updateAccountSettings() = silhouette.SecuredAction.async { implicit request =>
    accountSettingsForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.my.settings.account(formWithErrors, request.identity))),

      f =>
        users.updateUserSettings(request.identity.username, f.email, f.name, f.bio, f.website)
          .map { success =>
            if (success)
              Redirect(routes.AccountSettingsController.index).flashing("success" -> "Your settings have been saved.")
            else 
              Redirect(routes.AccountSettingsController.index).flashing("error" -> "There was an error while saving your settings.")
          }.recover { case t:Throwable => {
            t.printStackTrace()
            Redirect(routes.AccountSettingsController.index).flashing("error" -> "There was an error while saving your settings.")
          }}
    )
  }
  
  def deleteAccount() = silhouette.SecuredAction.async { implicit request =>
    deleteUserAccount(request.identity.username).flatMap { _ =>
      silhouette.env.authenticatorService.discard(
        request.authenticator,
        Redirect(controllers.landing.routes.LandingController.index))
    }
  }

}

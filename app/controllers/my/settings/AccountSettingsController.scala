package controllers.my.settings

import controllers.{ HasUserService, HasConfig, Security, WebJarAssets }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.user.Roles._
import models.user.UserService
import models.upload.UploadService
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.Controller
import scala.concurrent.{ ExecutionContext, Future }
import storage.Uploads

case class AccountSettingsData(email: String, name: Option[String], bio: Option[String], website: Option[String])

class AccountSettingsController @Inject() (
    val config: Configuration,
    val users: UserService,
    val uploadService: UploadService,
    val uploadStore: Uploads,
    val messagesApi: MessagesApi,
    implicit val webjars: WebJarAssets,
    implicit val ctx: ExecutionContext
  ) extends Controller with AuthElement with HasUserService with HasConfig with Security with I18nSupport {

  val accountSettingsForm = Form(
    mapping(
      "email" -> email,
      "name" -> optional(text(maxLength=80)),
      "bio" -> optional(text(maxLength=256)),
      "website" -> optional(text(maxLength=256))
    )(AccountSettingsData.apply)(AccountSettingsData.unapply)
  )

  def index() = StackAction(AuthorityKey -> Normal) { implicit request =>
    val form = accountSettingsForm.fill(AccountSettingsData(
      users.decryptEmail(loggedIn.user.getEmail),
      Option(loggedIn.user.getRealName),
      Option(loggedIn.user.getBio),
      Option(loggedIn.user.getWebsite)))
    
    Ok(views.html.my.settings.account(form, loggedIn.user))
  }

  def updateAccountSettings() = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    accountSettingsForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.my.settings.account(formWithErrors, loggedIn.user))),

      f =>
        users.updateUserSettings(loggedIn.user.getUsername, f.email, f.name, f.bio, f.website)
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
  
  def deleteAccount() = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    val username = loggedIn.user.getUsername
    
    val fDeleteUserDir = uploadStore.deleteUserDir(username)
    val fDeletePendingUpload = uploadService.deletePendingUpload(username)
    
    val f = for {
      _ <- fDeleteUserDir
      _ <- fDeletePendingUpload
      
      // TODO delete from DB: sharing policies
      // TODO delete from DB: documents and document_fileparts
      
      _ <- users.deleteByUsername(username)
    } yield ()
    
    f.map { _ =>
      
      // TODO log out
      // TODO redirect to good bye page
      
      Ok
    }
  }

}

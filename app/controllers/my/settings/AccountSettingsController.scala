package controllers.my.settings

import controllers.{ BaseAuthController, ControllerContext }
import javax.inject.Inject
import models.user.Roles._
import models.user.UserService
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, MessagesApi }
import scala.concurrent.Future

case class AccountSettingsData(email: String, name: Option[String], bio: Option[String], website: Option[String])

class AccountSettingsController @Inject() (implicit val ctx: ControllerContext, val messagesApi: MessagesApi)
  extends BaseAuthController with I18nSupport {
  
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
      loggedIn.user.getEmail,
      Option(loggedIn.user.getRealName),
      Option(loggedIn.user.getBio),
      Option(loggedIn.user.getWebsite)))
    
    Ok(views.html.my.settings.account(form))
  }

  def updateAccountSettings() = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    accountSettingsForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.my.settings.account(formWithErrors))),

      f =>
        UserService.updateUserSettings(loggedIn.user.getUsername, f.email, f.name, f.bio, f.website)
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

}

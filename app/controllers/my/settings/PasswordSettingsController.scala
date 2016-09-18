package controllers.my.settings

import controllers.{ HasConfig, HasUserService, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.user.UserService
import models.user.Roles._
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.Controller
import scala.concurrent.{ ExecutionContext, Future }

case class PasswordSettingsData(currentPassword: String, newPassword: String, verifyPassword: String)

class PasswordSettingsController @Inject() (
    val config: Configuration,
    val users: UserService,
    val messagesApi: MessagesApi,
    implicit val ctx: ExecutionContext
  ) extends Controller with AuthElement with HasConfig with HasUserService with Security with I18nSupport {

  private val matchingPasswords: Constraint[PasswordSettingsData] = Constraint("constraints.valid"){ d =>
    if (d.newPassword == d.verifyPassword) Valid else Invalid("Passwords must match")
  }

  val passwordSettingsForm = Form(
    mapping(
      "current" -> nonEmptyText,
      "new" -> nonEmptyText,
      "verify" -> nonEmptyText
    )(PasswordSettingsData.apply)(PasswordSettingsData.unapply).verifying(matchingPasswords)
  )

  def index() = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok(views.html.my.settings.password(passwordSettingsForm, loggedIn.user))
  }

  def updatePassword() = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    passwordSettingsForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.my.settings.password(formWithErrors, loggedIn.user))),

      f => {
        users.updatePassword(loggedIn.user.getUsername, f.currentPassword, f.newPassword)
          .map { _ match {
            case Right(_) =>
              Redirect(routes.PasswordSettingsController.index).flashing("success" -> "Your password has been updated.")
            case Left(errorMessage) =>
              Redirect(routes.PasswordSettingsController.index).flashing("error" -> errorMessage)
          }}.recover { case t:Throwable => {
            t.printStackTrace()
            Redirect(routes.PasswordSettingsController.index).flashing("error" -> "There was an error while updating your password.")
          }}
      }
    )
  }

}

package controllers.landing

import javax.inject.Inject
import models.user.UserService
import models.generated.tables.records.UserRecord
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.libs.mailer._
import play.api.mvc.{ Action, Controller }
import scala.concurrent.{ ExecutionContext, Future }

case class ResetPasswordData(email: String)

class ResetPasswordController @Inject() (
    val users: UserService,
    val messagesApi: MessagesApi,
    val mailerClient: MailerClient,
    implicit val ctx: ExecutionContext
  ) extends Controller with I18nSupport {
  
  val resetPasswordForm = Form(
    mapping("email" -> email)(ResetPasswordData.apply)(ResetPasswordData.unapply)
  )

  /** TODO make configurable **/
  private def sendMail(user: UserRecord, newPassword: String) = {
    val name = Option(user.getRealName).getOrElse(user.getUsername)
    
    val message =
      s"""Dear $name,
        |
        |Your Recogito password was reset to
        |
        |   $newPassword
        |
        |Please log in to Recogito with this password. You can then change it
        |in your personal Account Settings.
        |
        |Cheers,
        |the Recogito Team""".stripMargin
    
    val email = Email(
      "Your Recogito password was reset",
      "Pelagios Commons <commons@pelagios.org>",
      Seq(users.decryptEmail(user.getEmail)),
      Some(message)
    )
    
    mailerClient.send(email) 
  }
  
  def showResetForm = Action { implicit request =>
    Ok(views.html.landing.resetPassword(resetPasswordForm))
  }
  
  def resetPassword = Action.async { implicit request =>
    resetPasswordForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.landing.resetPassword(formWithErrors))),

      resetData => 
        users.findByEmail(resetData.email).flatMap {
          case Some(user) => 
            users.resetPassword(user.getUsername).map { password =>
              sendMail(user, password)
              Ok(views.html.landing.resetPasswordOk())
            }
                       
          case None =>
            Future.successful(Redirect(routes.ResetPasswordController.showResetForm()).flashing("error" -> "Can't find that e-mail, sorry."))
        }
    )
  }
  
}
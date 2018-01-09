package controllers.landing

import javax.inject.{Inject, Singleton}
import models.user.UserService
import models.generated.tables.records.UserRecord
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.mailer._
import play.api.mvc.{Action, AbstractController, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}

case class ResetPasswordData(email: String)

@Singleton
class ResetPasswordController @Inject() (
    val components: ControllerComponents,
    val users: UserService,
    val mailerClient: MailerClient,
    implicit val ctx: ExecutionContext
  ) extends AbstractController(components) with I18nSupport {
  
  val resetPasswordForm = Form(
    mapping("email" -> email)(ResetPasswordData.apply)(ResetPasswordData.unapply)
  )

  /** TODO make configurable **/
  private def sendMail(user: UserRecord, newPassword: String) = Future {
    val name = Option(user.getRealName).getOrElse(user.getUsername)
    
    val message =
      s"""Dear $name,
        |
        |Your Recogito password was reset to
        |
        |   $newPassword
        |
        |Please log in to Recogito with this password. You can change it
        |in your personal Account Settings.
        |
        |Cheers,
        |the Recogito Team""".stripMargin
    
    // TODO this now hard-wires "noreply@pelagios.org" as reply address
    // TODO see if we can take this directly from the config file instead
    val email = Email(
      "Your Recogito password was reset",
      "Recogito Team <noreply@pelagios.org>",
      Seq(users.decryptEmail(user.getEmail)),
      Some(message)
    )
    
    mailerClient.send(email)
  } recover { case t: Throwable =>
    t.printStackTrace
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
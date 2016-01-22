package controllers

import database.DB
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.{ Action, Controller }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.Form
import play.api.data.Forms._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import models.Users

case class SignupData(username: String, email: String, password: String)

/** Just temporary container for everything while we're still hacking on the basics **/
class Signup @Inject() (implicit db: DB) extends Controller {

  val signupForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText
    )(SignupData.apply)(SignupData.unapply)
  )

  def showSignupForm = Action {
    Ok(views.html.signup(signupForm))
  }

  def processSignup = Action { implicit request =>
    signupForm.bindFromRequest.fold(
      formWithErrors => {
        Logger.info("Bad request!")
        BadRequest(views.html.signup(formWithErrors))
      },

      signupData => {
        Users.insertUser(signupData.username, signupData.email, signupData.password)
        Redirect(routes.Application.landingPage()) // .flashing("success" -> "Account Created!")
      }
    )
  }

}

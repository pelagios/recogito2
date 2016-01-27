package controllers.landing

import controllers.{ AbstractController, Security }
import database.DB
import javax.inject.Inject
import jp.t2v.lab.play2.auth.Login
import models.UserService
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import scala.concurrent.Future

case class SignupData(username: String, email: String, password: String)

class SignupController @Inject() (implicit val db: DB) extends AbstractController with Login with Security {
  
  private val DEFAULT_ERROR_MESSAGE = "There was an error."
  
  // We're checking for occurrence of specific text snippets in exception message. 
  // Quite hard-coded... but currently don't see a cleaner way.
  private val SCREEN_ERROR_MESSAGES = Map(
    "UNIQUE constraint failed" -> "This username is no longer available")
  
  private def screenErrorMessage(t: Throwable) = 
    SCREEN_ERROR_MESSAGES.find { case (trigger, screenMsg) => t.getMessage.contains(trigger) }
      .map(_._2).getOrElse(DEFAULT_ERROR_MESSAGE)

  val signupForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText
    )(SignupData.apply)(SignupData.unapply)
  )

  def showSignupForm = Action {
    Ok(views.html.landing.signup(signupForm))
  }

  def processSignup = Action.async { implicit request =>
    signupForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.landing.signup(formWithErrors))),

      signupData =>
        UserService.insertUser(signupData.username, signupData.email, signupData.password)
          .flatMap(user => gotoLoginSucceeded(user.getUsername))
          .recover{ case t:Throwable =>
            Ok(views.html.landing.signup(signupForm.bindFromRequest, Some(screenErrorMessage(t)))) }
    )
  }

}

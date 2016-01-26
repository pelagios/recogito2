package controllers.landing

import controllers.{ AbstractController, Security }
import database.DB
import javax.inject.Inject
import jp.t2v.lab.play2.auth.Login
import models.Users
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import scala.concurrent.Future

case class SignupData(username: String, email: String, password: String)

class SignupController @Inject() (implicit val db: DB) extends AbstractController with Login with Security {

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
        Users.insertUser(signupData.username, signupData.email, signupData.password).flatMap(user =>
          gotoLoginSucceeded(user.getUsername))
    )
  }

}

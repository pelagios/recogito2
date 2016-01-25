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
import scala.concurrent.Future
import jp.t2v.lab.play2.auth.LoginLogout
import scala.concurrent.Await

case class SignupData(username: String, email: String, password: String)

/** Just temporary container for everything while we're still hacking on the basics **/
class Signup @Inject() (implicit val db: DB) extends Controller with LoginLogout with HasDB with AuthConfigImpl{

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

  def processSignup = Action.async { implicit request =>
    signupForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.signup(formWithErrors))),

      signupData =>
        Users.insertUser(signupData.username, signupData.email, signupData.password).flatMap(user =>
          gotoLoginSucceeded(user.getUsername))
    )
  }

}

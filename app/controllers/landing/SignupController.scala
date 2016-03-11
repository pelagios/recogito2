package controllers.landing

import controllers.{ AbstractController, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.Login
import models.user.UserService
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Action
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.Logger
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import storage.DB

case class SignupData(username: String, email: String, password: String)

class SignupController @Inject() (implicit val db: DB) extends AbstractController with Login with Security {

  private val DEFAULT_ERROR_MESSAGE = "There was an error."

  // Only alphanumeric + a small set of special characters are valid in usernames
  private val VALID_CHARACTERS =
    (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ Seq('_', '-', '.')).toSet
    
  // We use usernames as first-level URL elements, therefore some are prohibited (cf. routes file)
  private val RESERVED_NAMES =  
    Set("assets", "signup", "login", "logout", "admin", "document", "api")

  private val isNotReserved: Constraint[String] = Constraint("constraints.notreserved")({username =>
    if (RESERVED_NAMES.contains(username.toLowerCase))
      Invalid(username + " is a reserved word")
    else
      Valid
  })
    
  /** Username must be unique in DB, and may not contain special characters **/
  private val validAndAvailable: Constraint[String] = Constraint("constraints.valid")({ username =>
    // Check if username contains only valid characters
    val invalidChars = username.filter(!VALID_CHARACTERS.contains(_)).toSeq
    if (invalidChars.size == 0) {
      try {
        Await.result(UserService.findByUsernameIgnoreCase(username), 10.second) match {
          case Some(user) =>
            Invalid("This username is no longer available")
          case None =>
            Valid
        }
      } catch {
        case t: Throwable => {
          // Shouldn't happen, unless DB is broken
          t.printStackTrace()
          Invalid(DEFAULT_ERROR_MESSAGE)
        }
      }
    } else {
      Invalid("Please don't use special characters - " + invalidChars.mkString(", "))
    }
  })

  val signupForm = Form(
    mapping(
      "username" -> nonEmptyText(minLength=3).verifying(isNotReserved).verifying(validAndAvailable),
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
          .recover { case t:Throwable => {
            t.printStackTrace()
            Ok(views.html.landing.signup(signupForm.bindFromRequest, Some(DEFAULT_ERROR_MESSAGE)))
          }}
    )
  }

}

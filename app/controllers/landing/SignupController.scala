package controllers.landing

import controllers.{ HasConfig, HasUserService, Security }
import java.io.FileInputStream
import java.sql.Timestamp
import java.util.{ Date, UUID }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.{ AuthElement, Login }
import models.ContentType
import models.annotation.{ Annotation, AnnotationService }
import models.document.DocumentService
import models.user.UserService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.libs.json.{ Json, JsObject }
import play.api.mvc.{ Action, Controller }
import scala.concurrent.{ Await, Future, ExecutionContext }
import scala.concurrent.duration._

case class SignupData(username: String, email: String, password: String)

class SignupController @Inject() (
    val config: Configuration,
    val users: UserService,
    val messagesApi: MessagesApi,
    val annotations: AnnotationService,
    val documents: DocumentService,
    implicit val ctx: ExecutionContext
  ) extends Controller with AuthElement with HasUserService with HasConfig with Security with Login with I18nSupport {

  private val DEFAULT_ERROR_MESSAGE = "There was an error."

  // Only alphanumeric + a small set of special characters are valid in usernames
  private val VALID_CHARACTERS =
    (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ Seq('_', '-', '.')).toSet

  // We use usernames as first-level URL elements, therefore some are prohibited (cf. routes file)
  private val RESERVED_NAMES =
    Set("admin", "api", "assets", "document", "favicon.ico", "guest", "help", "login", "logout",
        "pelagios", "recogito", "reset_password", "signup", "settings", "webjars")

  private val isNotReserved: Constraint[String] = Constraint("constraints.notreserved")({username =>
    if (RESERVED_NAMES.contains(username.toLowerCase))
      Invalid(username + " is a reserved word")
    else
      Valid
  })

  /** Username must be unique in DB, and may not contain special characters **/
  private val usernameValidAndAvailable: Constraint[String] = Constraint("constraints.username.valid")({ username =>
    // Check if username contains only valid characters
    val invalidChars = username.filter(!VALID_CHARACTERS.contains(_)).toSeq
    if (invalidChars.size == 0) {
      try {
        Await.result(users.findByUsernameIgnoreCase(username), 10.second) match {
          case Some(userWithRoles) =>
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
  
  private val emailAvailable: Constraint[String] = Constraint("constraints.email.available")({ email =>
    Await.result(users.findByEmail(email), 10.second) match {
      case Some(user) => Invalid("This E-Mail is already in use")
      case None => Valid
    }
  })

  val signupForm = Form(
    mapping(
      "username" -> nonEmptyText(minLength=3, maxLength=16).verifying(isNotReserved).verifying(usernameValidAndAvailable),
      "email" -> email.verifying(emailAvailable),
      "password" -> nonEmptyText
    )(SignupData.apply)(SignupData.unapply)
  )
  
  private def importOnboardingContent(username: String) = {
    val document = new DocumentRecord(
      documents.generateRandomID(),
      username,
      new Timestamp(new Date().getTime),
      "Welcome to Recogito",
      "Recogito Team",
      null, // TODO timestamp_numeric
      null,
      null,
      null,
      null,
      null,
      null,
      false)
    
    val filepart = new DocumentFilepartRecord(
      UUID.randomUUID(),
      document.getId,
      "welcome.txt",
      ContentType.TEXT_PLAIN.toString,
      "welcome.txt",
      1,
      null)
    
    val fileInputStream = new FileInputStream("conf/onboarding/welcome.txt")

    val annotation = 
      Json.parse(new FileInputStream("conf/onboarding/welcome.json")).as[JsObject] ++
      Json.obj(
        "annotation_id" -> UUID.randomUUID(),
        "version_id" -> UUID.randomUUID(),
        "annotates" -> Json.obj(
          "document_id" -> document.getId,
          "filepart_id" -> filepart.getId,
          "content_type" -> filepart.getContentType
        )
      )

    for {
      _ <- documents.importDocument(document, Seq((filepart, fileInputStream)))    
      _ <- annotations.insertOrUpdateAnnotation(Json.fromJson[Annotation](annotation).get)
    } yield ()
  }

  def showSignupForm = Action {
    Ok(views.html.landing.signup(signupForm))
  }

  def processSignup = Action.async { implicit request =>
    signupForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.landing.signup(formWithErrors))),

      signupData => {
        val fUser = for {
          user <- users.insertUser(signupData.username, signupData.email, signupData.password)
          _ <- importOnboardingContent(user.getUsername)
        } yield user
        
        fUser
          .flatMap(user => gotoLoginSucceeded(user.getUsername))
          .recover { case t:Throwable => {
            t.printStackTrace()
            Ok(views.html.landing.signup(signupForm.bindFromRequest, Some(DEFAULT_ERROR_MESSAGE)))
          }}
      }
    )
  }

}

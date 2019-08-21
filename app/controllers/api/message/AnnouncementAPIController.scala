package controllers.api.message

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.mailer._
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.Try
import services.announcement.AnnouncementService
import services.document.DocumentService
import services.user.{User, UserService}

@Singleton
class AnnouncementAPIController @Inject() (
  val announcements: AnnouncementService,
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val silhouette: Silhouette[Security.Env],
  val users: UserService,
  val env: Environment,
  val mailerClient: MailerClient,
  implicit val ctx: ExecutionContext
) extends BaseAuthController(components, config, documents, users) with HasPrettyPrintJSON {

  private val MESSAGE_TEMPLATE =
    Try(Source.fromFile(env.getFile("conf/message-template.html"))).toOption.map { _.getLines.mkString("\n") }
      .getOrElse("Recogito user {{sender}} sent you a message: \n\n {{message}}")

  def myLatest = silhouette.SecuredAction.async { implicit request =>
    announcements.findLatestUnread(request.identity.username).map { _ match {
      case Some(message) => 
        Ok(Json.obj(
          "id" -> message.getId,
          "content" -> message.getContent
        ))

      case None => NotFound
    }}
  }

  def confirm(id: UUID) = silhouette.SecuredAction.async { implicit request =>
    announcements.confirm(id, request.identity.username, "OK").map { success => 
      if (success) Ok else BadRequest
    }
  }

  def sendMessage() = silhouette.SecuredAction.async { implicit request => 

    // Does the actual sending
    def send(sender: String, recipient: User, message: String) = {
      val text = MESSAGE_TEMPLATE
        .replace("{{base}}", "http://recogito.pelagios.org")
        .replace("{{sender}}", sender)
        .replace("{{message}}", message)

      // TODO this now hard-wires "noreply@pelagios.org" as reply address
      // TODO see if we can take this directly from the config file instead
      val email = Email(
        s"[Recogito] Message from user $sender",
        "Recogito Team <noreply@pelagios.org>",
        Seq(users.decryptEmail(recipient.email)),
        bodyHtml = Some(text)
      )

      play.api.Logger.info(s"Message from $sender to ${recipient.username}: $message")      
      mailerClient.send(email)
    }

    // Request validation and error handling
    request.body.asJson match {
      case Some(json) =>
        val maybeTo: Option[String] = (json \ "to").asOpt[String]
        val maybeMessage: Option[String] = (json \ "message").asOpt[String]

        (maybeTo, maybeMessage) match {
          case (Some(toUser), Some(message)) => 
            users.findByUsername(toUser).map { _ match {
              case Some(user) => 
                // TODO check if recipient != sender (but leave open for testing for now)
                send(request.identity.username, user, message)
                Ok

              case None => 
                NotFound // Unknown recipient name
            }}

          case _ =>
            Future.successful(BadRequest) // Lacks recipient name, message, or both
        }

      case None => 
        Future.successful(BadRequest) // No JSON payload
    }
  }

}

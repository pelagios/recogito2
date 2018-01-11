package controllers.my.settings

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{HasUserService, HasConfig, Security }
import controllers.document.{BackupReader, HasBackupValidation}
import java.io.File
import javax.inject.Inject
import services.annotation.AnnotationService
import services.document.DocumentService
import services.user.Roles._
import services.user.UserService
import play.api.{ Configuration, Logger }
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{ ExecutionContext, Future }
import transform.tiling.TilingService

class RestoreController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val annotations: AnnotationService,
  implicit val documents: DocumentService,
  implicit val tiling: TilingService,
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem
) extends AbstractController(components) 
    with HasUserService 
    with HasConfig
    with I18nSupport
    with BackupReader {
  
  def index() = silhouette.SecuredAction { implicit request =>
    Ok(views.html.my.settings.restore(request.identity))
  }

  def restore() = silhouette.SecuredAction.async { implicit request =>
    request.body.asMultipartFormData.map { tempfile =>
      tempfile.file("backup") match {
       
        case Some(filepart) =>
          // Forces the owner of the backup to the currently logged in user
          restoreBackup(
            filepart.ref.path.toFile,
            runAsAdmin = false,
            forcedOwner = Some(request.identity.username)
          ).map { _ => 
            Redirect(routes.RestoreController.index).flashing("success" -> "The document was restored successfully.") 
          }.recover { 
            
            case e: HasBackupValidation.InvalidSignatureException =>
              Redirect(routes.RestoreController.index).flashing("error" -> "The authenticity of your backup could not be verified.")
              
            case t: Throwable =>
              t.printStackTrace()
              Redirect(routes.RestoreController.index).flashing("error" -> "There was an error restoring your document.") 
          }
          
        case None =>
          Logger.warn("Personal document restore POST without file attached")
          Future.successful(BadRequest)        
      }
    }.getOrElse {
      Logger.warn("Personal document restore POST without form data")
      Future.successful(BadRequest)
    }
  }

}

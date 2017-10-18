package controllers.my.settings

import akka.actor.ActorSystem
import controllers.{ HasUserService, HasConfig, Security }
import controllers.document.{ BackupReader, HasBackupValidation }
import java.io.File
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.annotation.AnnotationService
import models.document.DocumentService
import models.user.Roles._
import models.user.UserService
import play.api.{ Configuration, Logger }
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.Controller
import scala.concurrent.{ ExecutionContext, Future }
import transform.tiling.TilingService

class RestoreController @Inject() (
    val config: Configuration,
    val users: UserService,
    val messagesApi: MessagesApi,
    implicit val annotations: AnnotationService,
    implicit val documents: DocumentService,
    implicit val tiling: TilingService,
    implicit val ctx: ExecutionContext,
    implicit val system: ActorSystem
) extends Controller 
    with AuthElement 
    with HasUserService 
    with HasConfig
    with Security
    with I18nSupport
    with BackupReader {
  
  def index() = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok(views.html.my.settings.restore(loggedIn))
  }

  def restore() = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    request.body.asMultipartFormData.map { tempfile =>
      tempfile.file("backup") match {
       
        case Some(filepart) =>
          // Forces the owner of the backup to the currently logged in user
          restoreBackup(
            filepart.ref.file,
            runAsAdmin = false,
            forcedOwner = Some(loggedIn.username)
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

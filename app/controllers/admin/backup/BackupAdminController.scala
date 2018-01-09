package controllers.admin.backup

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import controllers.document.BackupReader
import javax.inject.{Inject, Singleton}
import models.ContentType
import models.annotation.AnnotationService
import models.document.DocumentService
import models.generated.tables.records.DocumentFilepartRecord
import models.user.UserService
import models.user.Roles._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import transform.tiling.TilingService

@Singleton
class BackupAdminController @Inject() (
    val components: ControllerComponents,
    val config: Configuration,
    val users: UserService,
    val silhouette: Silhouette[Security.Env],
    implicit val tilingService: TilingService,
    implicit val annotations: AnnotationService,
    implicit val documents: DocumentService,
    implicit val ctx: ExecutionContext,
    implicit val system: ActorSystem,
    implicit val webJarsUtil: WebJarsUtil
  ) extends BaseAuthController(components, config, documents, users) with BackupReader {
  
  def index = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request => 
    Ok(views.html.admin.backup.index())
  }
  
  def restore = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    
    request.body.asMultipartFormData.flatMap(_.file("backup")) match {
      case Some(formData) =>
        restoreBackup(formData.ref.path.toFile, runAsAdmin = true, forcedOwner = None).map { case (doc, fileparts) =>          
          Redirect(routes.BackupAdminController.index)
        }.recover { case t: Throwable =>
          t.printStackTrace()
          InternalServerError
        }
        
      case None => 
        Future.successful(BadRequest)
    }
    
  }
  
}
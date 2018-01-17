package controllers.admin.maintenance

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import services.document.DocumentService
import services.upload.UploadService
import services.user.UserService
import services.user.Roles._

@Singleton
class MaintenanceController @Inject()(
  val components: ControllerComponents, 
  val config: Configuration,
  val documents: DocumentService,
  val silhouette: Silhouette[Security.Env],
  val uploads: UploadService,
  val users: UserService,
  implicit val ctx: ExecutionContext
) extends BaseAuthController(components, config, documents, users) {
  
  def index = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    uploads.listPendingUploads().map { uploads =>
      Ok(views.html.admin.maintenance.index(uploads))
    }
  }
  
  def deletePending(id: Int) = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request =>
    Ok
  }
  
  def deleteAllPending = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request =>
    Ok
  }
  
}
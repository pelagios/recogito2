package controllers.admin.maintenance

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import javax.inject.{Inject, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import services.announcement.AnnouncementService
import services.document.DocumentService
import services.upload.UploadService
import services.user.UserService
import services.user.Roles._

@Singleton
class MaintenanceController @Inject()(
  val announcements: AnnouncementService,
  val components: ControllerComponents, 
  val config: Configuration,
  val documents: DocumentService,
  val silhouette: Silhouette[Security.Env],
  val uploads: UploadService,
  val users: UserService,
  implicit val ctx: ExecutionContext,
  implicit val webJarsUtil: WebJarsUtil
) extends BaseAuthController(components, config, documents, users) {
  
  def index = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    uploads.listPendingUploads().map { uploads =>
      Ok(views.html.admin.maintenance.index(uploads))
    }
  }
  
  def deletePending(id: Int) = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request =>
    Ok
  }
  
  def deleteAllPending = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    uploads.deleteAllPendingUploads().map(_ => Ok)
  }
  
  def insertBroadcast = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    announcements.insertBroadcastAnnouncement(
    """<h1>Our new mailing list is online!</h1>
      <p>
        Like all of our Pelagios resources, Recogito is community-driven. Get the latest news, join the 
        discussion, and help improve them by 
        <a href="https://groups.google.com/d/forum/pelagios-announcements/" target="_blank">joining our 
        new mailing list</a>, or sign up to one of our 
        <a href="https://groups.google.com/d/forum/pelagios-announcements/" target="_blank">Working Groups</a>.
      </p>
    """).map(_ => Ok)
  }
  
  def deleteAllServiceAnnouncements = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    announcements.clearAll().map { success =>
      if (success) Ok
      else InternalServerError
    }
  }
  
}
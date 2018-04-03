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
    """<h1>Have a few moments?</h1>
      <p>
        We have created a short survey to find out more about our community - and we would love to 
        hear from you. Answering our survey shouldn't take more than 10 minutes.
      </p>
      <p>
        <a href="https://docs.google.com/forms/d/e/1FAIpQLScNN2BL60pwHkh-OFZZh2BsSdF6hRXUnjB6Ewb06fdqcTXiWg/viewform" target="_blank">
          Take our survey
        </a>
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
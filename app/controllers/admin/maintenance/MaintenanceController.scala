package controllers.admin.maintenance

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security, HasPrettyPrintJSON}
import java.io.File
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import org.webjars.play.WebJarsUtil
import play.api.{Configuration, Environment}
import play.api.mvc.ControllerComponents
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext
import services.announcement.AnnouncementService
import services.document.DocumentService
import services.generated.tables.records.UploadRecord
import services.upload.UploadService
import services.user.UserService
import services.user.Roles._
import services.HasDate
import storage.uploads.Uploads

@Singleton
class MaintenanceController @Inject()(
  val announcements: AnnouncementService,
  val components: ControllerComponents, 
  val config: Configuration,
  val documents: DocumentService,
  val env: Environment,
  val silhouette: Silhouette[Security.Env],
  val uploadService: UploadService,
  val uploadStorage: Uploads,
  val users: UserService,
  implicit val ctx: ExecutionContext,
  implicit val webJarsUtil: WebJarsUtil
) extends BaseAuthController(components, config, documents, users) with HasPrettyPrintJSON with HasDate {

  implicit val uploadRecordWrites: Writes[UploadRecord] = (
    (JsPath \ "owner").write[String] and
    (JsPath \ "title").write[String] and
    (JsPath \ "created_at").write[DateTime]
  )(upload => (
    upload.getOwner,
    upload.getTitle,
    new DateTime(upload.getCreatedAt)
  ))
  
  def index = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    uploadService.listPendingUploads().map { uploads =>
      Ok(views.html.admin.maintenance.index(uploads))
    }
  }

  def listPendingUploads = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request => 
    uploadService.listPendingUploads().map { uploads =>
      jsonOk(Json.toJson(uploads))
    }
  }
  
  def deletePending(id: Int) = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request =>
    Ok
  }
  
  def deleteAllPending = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    uploadService.deleteAllPendingUploads().map(_ => Ok)
  }

  def getFilestoreSize = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request => 
    uploadStorage.getTotalSize.map { size => 
      jsonOk(Json.obj("size" -> size))
    }
  }
  
  def getLogLocation = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request => 
    jsonOk(Json.obj("path" -> s"${env.rootPath.getAbsolutePath}${File.separator}logs"))
  }

  def insertBroadcast = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    announcements.insertBroadcastAnnouncement(
    """# Like Recogito? Vote for us!

We are excited that Recogito has been nominated for the [Digital Humanities Awards](http://dhawards.org/dhawards2018/voting/) this year. 
If you like Recogito, do consider voting for us.

Many thanks & happy annotating!
    """).map(_ => Ok)
  }
  
  def deleteAllServiceAnnouncements = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    announcements.clearAll().map { success =>
      if (success) Ok
      else InternalServerError
    }
  }
  
}
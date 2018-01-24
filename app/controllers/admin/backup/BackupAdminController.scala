package controllers.admin.backup

import akka.actor.ActorSystem
import akka.stream.scaladsl.FileIO
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import controllers.document.BackupReader
import javax.inject.{Inject, Singleton}
import services.ContentType
import services.annotation.AnnotationService
import services.document.DocumentService
import services.generated.tables.records.DocumentFilepartRecord
import services.user.UserService
import services.user.Roles._
import services.visit.VisitService
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.{ControllerComponents, ResponseHeader, Result}
import play.api.libs.Files.TemporaryFileCreator
import play.api.http.HttpEntity
import scala.concurrent.{ExecutionContext, Future}
import storage.migration.AnnotationMigrationUtil
import transform.tiling.TilingService

@Singleton
class BackupAdminController @Inject() (
    val components: ControllerComponents,
    val config: Configuration,
    val migrationUtil: AnnotationMigrationUtil,
    val users: UserService,
    val visits: VisitService,
    val silhouette: Silhouette[Security.Env],
    implicit val tilingService: TilingService,
    implicit val annotations: AnnotationService,
    implicit val documents: DocumentService,
    implicit val ctx: ExecutionContext,
    implicit val system: ActorSystem,
    implicit val tmpFileCreator: TemporaryFileCreator,
    implicit val webJarsUtil: WebJarsUtil
  ) extends BaseAuthController(components, config, documents, users) with BackupReader {
  
  def index = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>    
    val fVisitsTotal = visits.countTotal()
    val fVisits6Months = visits.countSince(DateTime.now() minusMonths 6)
    
    val f = for {
      vTotal <- fVisitsTotal
      v6Months <- fVisits6Months
    } yield (vTotal, v6Months)
    
    f.map { case (vTotal, v6Months) =>
      Ok(views.html.admin.backup.index(vTotal, v6Months))
    }
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
  
  def exportVisits = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>    
    visits.scrollExport().map { path =>
      val fmt = DateTimeFormat.forPattern("yyyy-MM-dd")
      val source = FileIO.fromPath(path)
      val filename = s"visits-exported-${fmt.print(DateTime.now)}.csv"
      Result(
        header = ResponseHeader(200, Map("Content-Disposition" -> s"""attachment; filename="${filename}"""")),
        body = HttpEntity.Streamed(source, None, Some("text/csv"))
      )
    }
  }
  
  def deleteVisitsOlderThan(date: Option[String]) = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    date match {
      case Some(_) =>
        Future.successful(BadRequest("User-provided dates not supported yet."))
      
      case _ => 
        val cutoffDate = DateTime.now minusMonths 6
        visits.deleteOlderThan(cutoffDate).map { success =>
          if (success) Ok("Done.")
          else InternalServerError("Something went wrong.")
        }
    }
  }
  
  def runMigration = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request =>
    migrationUtil.runMigration
    Ok("Good Luck.")
  }

}
package controllers.document.settings

import controllers.BaseController
import java.util.UUID
import javax.inject.Inject
import models.annotation.AnnotationService
import models.contribution.ContributionService
import models.generated.tables.records.DocumentRecord
import models.user.Roles._
import play.api.Logger
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.Future
import storage.DB

case class RollbackData(annotationId: UUID, versionId: UUID)

object RollbackData {

  implicit val rollbackDataReads: Reads[RollbackData] = (
    (JsPath \ "annotation_id").read[UUID] and
    (JsPath \ "version_id").read[UUID]
  )(RollbackData.apply _)

}

class SettingsController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {

  def rollbackByTime(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    request.body.asJson match {
      case Some(json) => {
        Json.fromJson[RollbackData](json) match {
          case s: JsSuccess[RollbackData] => {
            ContributionService.getContributions(s.get.annotationId, s.get.versionId).flatMap { 
              _.headOption match {
                case Some(contribution) => { 
                  val f = for {
                    rollbackSuccess <- AnnotationService.rollbackByTime(documentId, contribution.madeAt)
                    purgeHistorySuccess <- if (rollbackSuccess) ContributionService.deleteHistoryAfter(documentId, contribution.madeAt) else Future.successful(false)
                  } yield purgeHistorySuccess
                  
                  f.map(success => if (success) Status(200) else InternalServerError)
                }
                
                case None =>
                  Future.successful(NotFound)
              }
            }
          }
          
          case e: JsError => {
            Logger.warn("POST to /settings/rollback but invalid JSON: " + e.toString)
            Future.successful(BadRequest)
          }
        }
      }
        
      case None => {
        Logger.warn("POST to /settings/rollback but no JSON payload")
        Future.successful(BadRequest)
      }
      
    }
  }
  
  def showDocumentSettings(documentId: String, tab: Option[String]) = AsyncStack(AuthorityKey -> Normal) { implicit request =>    
    renderDocumentResponse(documentId, loggedIn.user.getUsername,
      { case (document, fileparts) =>
        tab.map(_.toLowerCase) match {
          case Some(t) if t == "sharing" =>
            Ok(views.html.document.settings.sharing(loggedIn.user.getUsername, document))
            
          case Some(t) if t == "history" =>
            Ok(views.html.document.settings.history(loggedIn.user.getUsername, document))
            
          case _ =>
            Ok(views.html.document.settings.metadata(loggedIn.user.getUsername, document)) 
        }
      }
    )
  }

}

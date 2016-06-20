package controllers.document.settings.rollback

import controllers.BaseController
import models.annotation.AnnotationService
import models.contribution.ContributionService
import models.user.Roles._
import java.util.UUID
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

case class RollbackData(annotationId: UUID, versionId: UUID)

object RollbackData {

  implicit val rollbackDataReads: Reads[RollbackData] = (
    (JsPath \ "annotation_id").read[UUID] and
    (JsPath \ "version_id").read[UUID]
  )(RollbackData.apply _)

}

trait RollbackActions { self: BaseController =>
  
  def rollbackByTime(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    request.body.asJson match {
      case Some(json) => Json.fromJson[RollbackData](json) match {
        case s: JsSuccess[RollbackData] =>
          ContributionService.getContributions(s.get.annotationId, s.get.versionId).flatMap { 
            _.headOption match {
              case Some(contribution) => { 
                val f = for {
                  rollbackSuccess <- AnnotationService.rollbackToTimestamp(documentId, contribution.madeAt)
                  purgeHistorySuccess <- if (rollbackSuccess) ContributionService.deleteHistoryAfter(documentId, contribution.madeAt) else Future.successful(false)
                } yield purgeHistorySuccess
                
                f.map(success => if (success) Status(200) else InternalServerError)
              }
              
              case None =>
                Future.successful(NotFound)
            }
        }
        
        case e: JsError => {
          Logger.warn("POST to /settings/rollback but invalid JSON: " + e.toString)
          Future.successful(BadRequest)
        }
      }
        
      case None => {
        Logger.warn("POST to /settings/rollback but no JSON payload")
        Future.successful(BadRequest)
      }
      
    }
  }
  
}
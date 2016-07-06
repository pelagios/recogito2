package controllers.document.settings.actions

import controllers.BaseAuthController
import controllers.document.settings.HasAdminAction
import java.util.UUID
import models.annotation.AnnotationService
import models.contribution.ContributionService
import models.user.Roles._
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

trait RollbackActions extends HasAdminAction { self: BaseAuthController =>
  
  def rollbackByTime(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
 
    jsonDocumentAdminAction[RollbackData](documentId, loggedIn.user.getUsername, { case (document, rollbackData) =>
      // Fetch the latest contribution for this annotation version, so we get the timestamp to roll back to
      ContributionService.getContributions(rollbackData.annotationId, rollbackData.versionId).flatMap {
        _.headOption match {
          case Some(contribution) => { 
            val f = for {
              rollbackSuccess <- AnnotationService.rollbackToTimestamp(documentId, contribution.madeAt)
              purgeHistorySuccess <- if (rollbackSuccess) ContributionService.deleteHistoryAfter(documentId, contribution.madeAt) else Future.successful(false)
            } yield purgeHistorySuccess
            
            f.map(success => if (success) Status(200) else InternalServerError)
          }
          
          case None => Future.successful(NotFound)
        }
      }
      
    })
  }
  
}
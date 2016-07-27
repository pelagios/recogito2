package controllers.document.settings.actions

import controllers.document.settings.SettingsController
import java.util.UUID
import models.user.Roles._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.Future

case class RollbackData(annotationId: UUID, versionId: UUID)

object RollbackData {

  implicit val rollbackDataReads: Reads[RollbackData] = (
    (JsPath \ "annotation_id").read[UUID] and
    (JsPath \ "version_id").read[UUID]
  )(RollbackData.apply _)

}

trait RollbackActions { self: SettingsController =>

  def rollbackByTime(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    jsonDocumentAdminAction[RollbackData](documentId, loggedIn.user.getUsername, { case (document, rollbackData) =>
      // Fetch the latest contribution for this annotation version, so we get the timestamp to roll back to
      contributions.getContributions(rollbackData.annotationId, rollbackData.versionId).flatMap {
        _.headOption match {
          case Some(contribution) => { 
            val f = for {
              rollbackSuccess <- annotations.rollbackToTimestamp(documentId, contribution.madeAt)
              purgeHistorySuccess <- if (rollbackSuccess) contributions.deleteHistoryAfter(documentId, contribution.madeAt) else Future.successful(false)
            } yield purgeHistorySuccess
            
            f.map(success => if (success) Status(200) else InternalServerError)
          }
          
          case None => Future.successful(NotFoundPage)
        }
      }
      
    })
  }
  
}
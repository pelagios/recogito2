package controllers.document.settings.actions

import controllers.document.settings.SettingsController
import models.user.Roles._
import play.api.libs.json.Json
import scala.concurrent.Future

trait HistoryActions { self: SettingsController =>
  
  def getContributionHistory(documentId: String, offset: Int, size: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    contributions.getHistory(documentId, offset, size).map(contributions => jsonOk(Json.toJson(contributions)))
  }

  def rollbackByTime(documentId: String, contributionId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentAdminAction(documentId, loggedIn.user.getUsername, { _ =>
      contributions.findById(contributionId).flatMap {
        case Some((contribution, _)) => {
          val f = for {
            rollbackSuccess <- annotations.rollbackToTimestamp(documentId, contribution.madeAt)
            purgeHistorySuccess <- if (rollbackSuccess) contributions.deleteHistoryAfter(documentId, contribution.madeAt) else Future.successful(false)
          } yield purgeHistorySuccess

          f.map(success => if (success) Ok else InternalServerError)
        }

        case None => Future.successful(NotFound)
      }
      
    })
  }
  
}
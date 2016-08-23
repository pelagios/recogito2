package controllers.document.settings.actions

import controllers.document.settings.SettingsController
import models.user.Roles._
import scala.concurrent.Future

trait RollbackActions { self: SettingsController =>

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
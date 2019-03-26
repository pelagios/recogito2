package controllers.document.settings.actions

import controllers.document.settings.SettingsController
import services.RuntimeAccessLevel
import services.generated.tables.records.DocumentRecord
import services.user.Roles._
import play.api.mvc.{ AnyContent, Request }
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.util.Try

trait DeleteActions { self: SettingsController =>
  
  protected def documentOwnerAction(docId: String, username: String, action: DocumentRecord => Future[Boolean]) = {
    documents.getDocumentRecordById(docId, Some(username)).flatMap(_ match {
      case Some((document, accesslevel)) => {
        if (accesslevel == RuntimeAccessLevel.OWNER) // We allow only the owner to delete a document
          action(document).map { success =>
            if (success) Ok else InternalServerError
          }
        else
          Future.successful(ForbiddenPage)
      }

      case None =>
        Future.successful(NotFoundPage) // No document with that ID found in DB
    }).recover { case t =>
      t.printStackTrace()
      InternalServerError(t.getMessage)
    }
  }

  /** Deletes all annotations on the document with the given ID **/
  def deleteAnnotations(docId: String) = self.silhouette.SecuredAction.async { implicit request =>
    documentOwnerAction(docId, request.identity.username, { document =>
      val deleteAnnotations = annotations.deleteByDocId(docId)
      val deleteContributions = contributions.deleteHistory(docId) 
      for {
        s1 <- deleteAnnotations
        s2 <- deleteContributions
      } yield (s1 && s2)
    })
  }

}

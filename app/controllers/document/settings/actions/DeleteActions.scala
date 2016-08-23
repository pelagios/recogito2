package controllers.document.settings.actions

import controllers.document.settings.SettingsController
import models.document.{ DocumentService, DocumentAccessLevel }
import models.user.Roles._
import scala.concurrent.Future

trait DeleteActions { self: SettingsController =>
  
  def deleteDocument(docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documents.getDocumentRecord(docId, Some(loggedIn.user.getUsername)).flatMap(_ match {
      case Some((document, accesslevel)) => {
        if (accesslevel == DocumentAccessLevel.OWNER) // We allow only the owner to delete a document
          for {
            _ <- documents.delete(document)
            success <- annotations.deleteByDocId(docId)
          } yield if (success) Status(200) else InternalServerError
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
  
}
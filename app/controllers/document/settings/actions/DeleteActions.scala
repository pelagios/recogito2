package controllers.document.settings.actions

import controllers.BaseAuthController
import controllers.document.settings.HasAdminAction
import models.annotation.AnnotationService
import models.document.{ DocumentService, DocumentAccessLevel }
import models.user.Roles._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

trait DeleteActions extends HasAdminAction { self: BaseAuthController =>
  
  def deleteDocument(docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    DocumentService.findById(docId, Some(loggedIn.user.getUsername))(self.db).flatMap(_ match {
      case Some((document, accesslevel)) => {
        if (accesslevel == DocumentAccessLevel.OWNER) // We allow only the owner to delete a document
          for {
            _ <- DocumentService.delete(document)(self.db)
            success <- AnnotationService.deleteByDocId(docId)
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
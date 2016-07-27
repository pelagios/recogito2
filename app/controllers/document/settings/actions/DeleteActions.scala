package controllers.document.settings.actions

import controllers.BaseAuthController
import controllers.document.settings.HasAdminAction
import models.annotation.AnnotationService
import models.document.{ DocumentService, DocumentAccessLevel }
import models.user.Roles._
import scala.concurrent.Future

trait DeleteActions extends HasAdminAction { self: BaseAuthController =>
  
  private implicit val executionContext = self.ctx.ec
  private implicit val elasticSearch = self.ctx.es
  
  def deleteDocument(docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    /*
    DocumentService.findById(docId, Some(loggedIn.user.getUsername))(self.ctx.db).flatMap(_ match {
      case Some((document, accesslevel)) => {
        if (accesslevel == DocumentAccessLevel.OWNER) // We allow only the owner to delete a document
          for {
            _ <- DocumentService.delete(document)(self.ctx.db)
            success <- annotationService.deleteByDocId(docId)
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
    */
    null
  }
  
}
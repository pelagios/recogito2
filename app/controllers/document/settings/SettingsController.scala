package controllers.document.settings

import controllers.BaseController
import java.util.UUID
import javax.inject.Inject
import models.annotation.AnnotationService
import models.contribution.ContributionService
import models.generated.tables.records.DocumentRecord
import models.user.Roles._
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import storage.DB

class SettingsController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {
  
  // TODO implement rollbackByUser
  
  def rollbackByTime(documentId: String, annotationId: UUID, versionId: UUID) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    AnnotationService.findVersionById(annotationId, versionId).flatMap {
      _ match {
        case Some(annotation) =>
          AnnotationService.rollbackByTime(documentId, annotation.lastModifiedAt)
            .map(success => if (success) Status(200) else InternalServerError)
        
        case None =>
          Future.successful(NotFound)
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

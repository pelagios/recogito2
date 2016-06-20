package controllers.document.settings

import controllers.BaseController
import java.util.UUID
import javax.inject.Inject
import models.annotation.AnnotationService
import models.contribution.ContributionService
import models.document.DocumentAccessLevel
import models.generated.tables.records.DocumentRecord
import models.user.Roles._
import play.api.Logger
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.Future
import storage.DB
import controllers.document.settings.sortorder.SortOrderSettings
import controllers.document.settings.access.AccessSettings
import controllers.document.settings.rollback.RollbackActions
import models.document.DocumentService

case class CollaboratorStub(collaborator: String, accessLevel: Option[DocumentAccessLevel])

object CollaboratorStub {
  
  implicit val collaboratorStubReads: Reads[CollaboratorStub] = (
    (JsPath \ "collaborator").read[String] and
    (JsPath \ "access_level").readNullable[DocumentAccessLevel]
  )(CollaboratorStub.apply _)
  
}

class SettingsController @Inject() (implicit val cache: CacheApi, val db: DB)
  extends BaseController with SortOrderSettings with AccessSettings with RollbackActions {
  
  def addCollaborator(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderAsyncDocumentResponse(documentId, loggedIn.user.getUsername, { case (document, fileparts, accesslevel) => 
      request.body.asJson match {
        case Some(json) => Json.fromJson[CollaboratorStub](json) match {
          case s: JsSuccess[CollaboratorStub] => {
            Logger.info("Add collaborator " + s.get.collaborator)
            null
          }
          
          case e: JsError => {
            Logger.warn("POST to /settings/add-collaborator but invalid JSON: " + e.toString)
            Future.successful(BadRequest)
          }
        }
          
        case None => {
          Logger.warn("POST to /settings/rollback but no JSON payload")
          Future.successful(BadRequest)
        }
      }
    })
  }
  
  /*
  def documentAdminAction(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderAsyncDocumentResponse(documentId, loggedIn.user.getUsername, { case (document, fileparts, accesslevel) =>
      if (document.g
    }
  }
  * 
  */
  
  def showDocumentSettings(documentId: String, tab: Option[String]) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    
    /** TODO settings only visible to OWNER and ADMIN **/
    
    renderAsyncDocumentResponse(documentId, loggedIn.user.getUsername, { case (document, fileparts, accesslevel) =>
      tab.map(_.toLowerCase) match { 
        case Some(t) if t == "sharing" => {
          val f = for {
            sharingPolicies <- DocumentService.listDocumentCollaborators(documentId)
          } yield sharingPolicies
           
          f.map(t => (Ok(views.html.document.settings.sharing(loggedIn.user.getUsername, document, t))))
        }
            
        case Some(t) if t == "history" =>
          Future.successful(Ok(views.html.document.settings.history(loggedIn.user.getUsername, document)))
            
        case _ =>
          Future.successful(Ok(views.html.document.settings.metadata(loggedIn.user.getUsername, document))) 
      }
    })
  }

}

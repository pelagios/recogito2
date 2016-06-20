package controllers.document.settings.actions

import controllers.BaseController
import controllers.document.settings.HasAdminAction
import models.document.{ DocumentAccessLevel, DocumentService }
import models.user.Roles._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

case class CollaboratorStub(collaborator: String, accessLevel: Option[DocumentAccessLevel])

object CollaboratorStub {
  
  implicit val collaboratorStubReads: Reads[CollaboratorStub] = (
    (JsPath \ "collaborator").read[String] and
    (JsPath \ "access_level").readNullable[DocumentAccessLevel]
  )(CollaboratorStub.apply _)
  
}

trait SharingActions extends HasAdminAction { self: BaseController =>
    
  def setIsPublic(documentId: String, enabled: Boolean) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentAdminAction(documentId, loggedIn.user.getUsername, { document =>
      DocumentService.setPublicVisibility(document.getId, enabled)(self.db).map(_ => Status(200))
    })
  }
  
  def addCollaborator(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    jsonDocumentAdminAction[CollaboratorStub](documentId, loggedIn.user.getUsername, { case (document, collaborator) =>
      // TODO implement
      Future.successful(Status(200))
    })
  }
  
  def removeCollaborator(documentId: String, username: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentAdminAction(documentId, loggedIn.user.getUsername, { document =>      
      DocumentService.removeDocumentCollaborator(documentId, username)(self.db).map(success =>
        if (success) Status(200) else InternalServerError)
    })
  }
  
}
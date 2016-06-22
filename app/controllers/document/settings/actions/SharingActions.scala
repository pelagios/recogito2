package controllers.document.settings.actions

import controllers.BaseController
import controllers.document.settings.HasAdminAction
import models.document.{ DocumentAccessLevel, DocumentService }
import models.generated.tables.records.SharingPolicyRecord
import models.user.Roles._
import models.user.UserService
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import controllers.HasPrettyPrintJSON

case class CollaboratorStub(collaborator: String, accessLevel: Option[DocumentAccessLevel])

object CollaboratorStub {
  
  implicit val collaboratorStubFormat: Format[CollaboratorStub] = (
    (JsPath \ "collaborator").format[String] and
    (JsPath \ "access_level").formatNullable[DocumentAccessLevel]
  )(CollaboratorStub.apply, unlift(CollaboratorStub.unapply))
  
  def fromSharingPolicy(policy: SharingPolicyRecord) = 
    CollaboratorStub(policy.getSharedWith, DocumentAccessLevel.withName(policy.getAccessLevel))
  
}

trait SharingActions extends HasAdminAction with HasPrettyPrintJSON { self: BaseController =>
    
  def setIsPublic(documentId: String, enabled: Boolean) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentAdminAction(documentId, loggedIn.user.getUsername, { document =>
      DocumentService.setPublicVisibility(document.getId, enabled)(self.db).map(_ => Status(200))
    })
  }
  
  def addCollaborator(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    val currentUser = loggedIn.user.getUsername    
    jsonDocumentAdminAction[CollaboratorStub](documentId, currentUser, { case (document, stub) =>
      if (stub.collaborator == document.getOwner) {
        // Doc owner as collaborator wouldn't make sense
        Future.successful(BadRequest)
      } else {
        // If no access level given, use READ as minimum default
        val accessLevel = stub.accessLevel.getOrElse(DocumentAccessLevel.READ)
        UserService.findByUsername(stub.collaborator)(self.db, self.cache).flatMap { _ match {
          case Some(userWithRoles) => 
            DocumentService.addDocumentCollaborator(documentId, currentUser, userWithRoles.user.getUsername, accessLevel)(self.db)
              .map(policy => jsonOk(Json.toJson(CollaboratorStub.fromSharingPolicy(policy))))
            
          case None => 
            Future.successful(NotFound)
        }}
      }
    })
  }
  
  def removeCollaborator(documentId: String, username: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentAdminAction(documentId, loggedIn.user.getUsername, { document =>      
      DocumentService.removeDocumentCollaborator(documentId, username)(self.db).map(success =>
        if (success) Status(200) else InternalServerError)
    })
  }
  
  def searchUsers(documentId: String, query: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UserService.searchUsers(query)(self.db).map { matches =>
      jsonOk(Json.toJson(matches))
    }
  }
  
}
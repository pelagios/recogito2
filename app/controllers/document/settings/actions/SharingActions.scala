package controllers.document.settings.actions

import controllers.HasPrettyPrintJSON
import controllers.document.settings.SettingsController
import models.document.DocumentAccessLevel
import models.generated.tables.records.SharingPolicyRecord
import models.user.Roles._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.Future

case class CollaboratorStub(collaborator: String, accessLevel: Option[DocumentAccessLevel], newCollaborator: Boolean)

object CollaboratorStub {
  
  implicit val collaboratorStubFormat: Format[CollaboratorStub] = (
    (JsPath \ "collaborator").format[String] and
    (JsPath \ "access_level").formatNullable[DocumentAccessLevel] and
    (JsPath \ "new_collaborator").formatNullable[Boolean]
      // Map between boolean and optional (with default = false)
      .inmap[Boolean](_.getOrElse(false), Some(_))
  )(CollaboratorStub.apply, unlift(CollaboratorStub.unapply))
  
  def fromSharingPolicy(policy: SharingPolicyRecord, isNewCollaborator: Boolean) = 
    CollaboratorStub(policy.getSharedWith, DocumentAccessLevel.withName(policy.getAccessLevel), isNewCollaborator)
  
}

trait SharingActions extends HasPrettyPrintJSON { self: SettingsController =>
    
  def setIsPublic(documentId: String, enabled: Boolean) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentAdminAction(documentId, loggedIn.user.getUsername, { case (document, _) =>
      documents.setPublicVisibility(document.getId, enabled).map(_ => Status(200))
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
        users.findByUsername(stub.collaborator).flatMap { _ match {
          case Some(userWithRoles) => 
            documents.addDocumentCollaborator(documentId, currentUser, userWithRoles.user.getUsername, accessLevel)
              .map { case (policy, isNew) => jsonOk(Json.toJson(CollaboratorStub.fromSharingPolicy(policy, isNew))) }
            
          case None => 
            Future.successful(NotFoundPage)
        }}
      }
    })
  }
  
  def removeCollaborator(documentId: String, username: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentAdminAction(documentId, loggedIn.user.getUsername, { case (document, _) =>      
      documents.removeDocumentCollaborator(documentId, username).map(success =>
        if (success) Status(200) else InternalServerError)
    })
  }
  
  def searchUsers(documentId: String, query: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    users.searchUsers(query).map { matches =>
      jsonOk(Json.toJson(matches))
    }
  }
  
}
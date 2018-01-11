package controllers.document.settings.actions

import controllers.document.settings.SettingsController
import services.document.DocumentAccessLevel
import services.generated.tables.records.SharingPolicyRecord
import services.user.Roles._
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

trait SharingActions { self: SettingsController =>
    
  def setIsPublic(documentId: String, enabled: Boolean) = self.silhouette.SecuredAction.async { implicit request =>
    documentAdminAction(documentId, request.identity.username, { doc =>
      // Make sure the license allows public access
      if (doc.license.map(_.isOpen).getOrElse(false))
        documents.setPublicVisibility(documentId, enabled).map(_ => Status(200))
      else
        // Note: setting is_public=true for a close document is not possible through UI!
        Future.successful(BadRequest)
    })
  }
  
  def addCollaborator(documentId: String) = self.silhouette.SecuredAction.async { implicit request =>
    val currentUser = request.identity.username    
    jsonDocumentAdminAction[CollaboratorStub](documentId, currentUser, { case (document, stub) =>
      if (stub.collaborator == document.getOwner) {
        // Doc owner as collaborator wouldn't make sense
        Future.successful(BadRequest)
      } else {
        // If no access level given, use READ as minimum default
        val accessLevel = stub.accessLevel.getOrElse(DocumentAccessLevel.READ)
        users.findByUsername(stub.collaborator).flatMap { _ match {
          case Some(user) => 
            documents.addDocumentCollaborator(documentId, currentUser, user.username, accessLevel)
              .map { case (policy, isNew) => jsonOk(Json.toJson(CollaboratorStub.fromSharingPolicy(policy, isNew))) }
            
          case None => 
            Future.successful(NotFoundPage)
        }}
      }
    })
  }
  
  def removeCollaborator(documentId: String, username: String) = self.silhouette.SecuredAction.async { implicit request =>
    documentAdminAction(documentId, request.identity.username, { _ =>      
      documents.removeDocumentCollaborator(documentId, username).map(success =>
        if (success) Status(200) else InternalServerError)
    })
  }
  
  def searchUsers(documentId: String, query: String) = self.silhouette.SecuredAction.async { implicit request =>
    users.searchUsers(query).map { matches =>
      jsonOk(Json.toJson(matches))
    }
  }
  
}
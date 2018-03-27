package controllers.document.settings.actions

import controllers.document.settings.SettingsController
import services.document.{DocumentInfo, SharingLevel, PublicAccess}
import services.generated.tables.records.SharingPolicyRecord
import services.user.Roles._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.mvc.Result
import scala.concurrent.Future
import scala.util.Try

case class CollaboratorStub(collaborator: String, accessLevel: Option[SharingLevel], newCollaborator: Boolean)

object CollaboratorStub {
  
  implicit val collaboratorStubFormat: Format[CollaboratorStub] = (
    (JsPath \ "collaborator").format[String] and
    (JsPath \ "access_level").formatNullable[SharingLevel] and
    (JsPath \ "new_collaborator").formatNullable[Boolean]
      // Map between boolean and optional (with default = false)
      .inmap[Boolean](_.getOrElse(false), Some(_))
  )(CollaboratorStub.apply, unlift(CollaboratorStub.unapply))
  
  def fromSharingPolicy(policy: SharingPolicyRecord, isNewCollaborator: Boolean) = 
    CollaboratorStub(policy.getSharedWith, SharingLevel.withName(policy.getAccessLevel), isNewCollaborator)
  
}

trait SharingActions { self: SettingsController =>
  
  def publicAccessAction(docId: String, username: String, fn: DocumentInfo => Future[Result]) =
    documentAdminAction(docId, username, { doc =>
      // Make sure the document has an open license - otherwise public access options cannot be changed
      val hasOpenLicense = doc.license.map(_.isOpen).getOrElse(false)
      if (hasOpenLicense)
        fn(doc)
      else
        // Note: changing the setting for a closed document is not possible through the UI
        Future.successful(BadRequest)
    })
    
  def setPublicVisibility(docId: String, v: String) = self.silhouette.SecuredAction.async { implicit request =>
    Try(PublicAccess.Visibility.withName(v)).toOption match {
      case Some(visibility) =>
        publicAccessAction(docId, request.identity.username, { doc =>
          val accessLevel = (doc.publicAccessLevel, visibility) match {
            // Keep access level pre-set by the user, if any
            case (Some(level), _) => Some(level)
            
            // No pre-set accesslevel and private visibility -> no access
            case (None, PublicAccess.PRIVATE) => None
            
            // No pre-set accesslevel and public visibility -> read-only
            case (None, _) => Some(PublicAccess.READ_ALL)
          }
          
          documents.setPublicAccessOptions(docId, visibility, accessLevel).map(_ => Ok)
        })
        
      case None =>
        // Submitting an invalid value is not possible through the UI
        Future.successful(BadRequest)
    }
  }
  
  def setPublicAccessLevel(docId: String, l: String) = self.silhouette.SecuredAction.async { implicit request =>
    Try(PublicAccess.AccessLevel.withName(l)).toOption.flatten match {
      case Some(accessLevel) =>
        publicAccessAction(docId, request.identity.username, { doc =>
          documents.setPublicAccessLevel(docId, Some(accessLevel)).map(_ => Ok)          
        })
        
      case None =>
        // Submitting an invalid value is not possible through the UI
        Future.successful(BadRequest)
    }  
  }
  
  def addCollaborator(documentId: String) = self.silhouette.SecuredAction.async { implicit request =>
    val currentUser = request.identity.username    
    jsonDocumentAdminAction[CollaboratorStub](documentId, currentUser, { case (document, stub) =>
      if (stub.collaborator == document.getOwner) {
        // Doc owner as collaborator wouldn't make sense
        Future.successful(BadRequest)
      } else {
        // If no access level given, use READ as minimum default
        val accessLevel = stub.accessLevel.getOrElse(SharingLevel.READ)
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
package controllers.my.sharing

import controllers.{Security, HasPrettyPrintJSON}
import com.mohiva.play.silhouette.api.Silhouette
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import services.PublicAccess
import services.folder.FolderService
import services.user.UserService

@Singleton
class SharingController @Inject() (
  components: ControllerComponents,
  folders: FolderService,
  silhouette: Silhouette[Security.Env],
  users: UserService,
  implicit val ctx: ExecutionContext
) extends AbstractController(components) with HasPrettyPrintJSON {
  
  def searchUsers(query: String) = silhouette.SecuredAction.async { implicit request =>
    users.searchUsers(query, 10).map { matches =>
      jsonOk(Json.toJson(matches))
    }
  }

  // TODO restrict to folder owners and admins
  def getFolderVisibility(id: UUID) = silhouette.SecuredAction.async { implicit request => 
    folders.getFolder(id).map { _ match {
      case Some(folder) => 
        if (folder.getOwner == request.identity.username)
          jsonOk(Json.obj(
            "id" -> folder.getId,
            "visibility" -> folder.getPublicVisibility,
            "access_level" -> folder.getPublicAccessLevel
          ))
        else 
          Forbidden

      case None => 
        NotFound
    }}
  }

  // TODO restrict to folder owners and admins
  def setFolderVisibility() = silhouette.SecuredAction.async { implicit request =>
    request.body.asJson match {
      case Some(json) => 
        val id = (json \ "ids").as[Seq[UUID]]
        val visibility = (json \ "visibility").asOpt[String]
        val accessLevel = (json \ "access_level").asOpt[String]

        // TODO recursive!

        val fUpdateVisibility = visibility.map { v => 
          folders.updatePublicVisibility(id, PublicAccess.Visibility.withName(v))
        }

        val fUpdateAccessLevel = accessLevel.map { a => 
          folders.updatePublicAccessLevel(id, PublicAccess.AccessLevel.withName(a).get)
        }

        val f = Future.sequence(Seq(fUpdateVisibility, fUpdateAccessLevel).flatten)
        f.map { _ => Ok }
        
      case None =>
        Future.successful(BadRequest)
    }
    
    /*  
    folders.getChildrenRecursive(id).map { idsAndTitles =>
      Ok
    }
    */
  }

  /** Currently restricted to sharing a single folder only **/
  def addFolderCollaborator() = silhouette.SecuredAction.async { implicit request => ???
    /*
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
    */
  }

  def removeFolderCollaborator() = silhouette.SecuredAction.async { implicit request => ??? 
  
  }

  // TODO set public access for (list of) document(s)

  // TODO add collaborators to document(s)

  // TODO remove collaborators from document(s)

  /* reminder - sharing policy schema:

    CREATE TABLE sharing_policy (
      id SERIAL PRIMARY KEY,
      -- one of the following two needs to be defined
      folder_id UUID REFERENCES folder(id),
      document_id TEXT REFERENCES document(id),
      shared_by TEXT NOT NULL REFERENCES "user"(username),
      shared_with TEXT NOT NULL REFERENCES "user"(username),
      shared_at TIMESTAMP WITH TIME ZONE NOT NULL,
      access_level TEXT NOT NULL,
      UNIQUE (document_id, shared_with)
    );

  */


}

package controllers.my.sharing

import controllers.{Security, HasPrettyPrintJSON}
import com.mohiva.play.silhouette.api.Silhouette
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import services.{PublicAccess, SharingLevel}
import services.document.DocumentService
import services.folder.FolderService
import services.user.UserService

@Singleton
class SharingController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[Security.Env],
  users: UserService,
  implicit val ctx: ExecutionContext,
  implicit val documents: DocumentService,
  implicit val folders: FolderService
) extends AbstractController(components) 
    with HasPrettyPrintJSON 
    with helpers.AddCollaboratorHelper
    with helpers.SetVisibilityHelper {
  
  def searchUsers(query: String) = silhouette.SecuredAction.async { implicit request =>
    users.searchUsers(query, 10).map { matches =>
      jsonOk(Json.toJson(matches))
    }
  }

  // TODO show only to folder owners and collaborators?
  def getFolderVisibility(id: UUID) = silhouette.SecuredAction.async { implicit request => 
    folders.getFolder(id, request.identity.username).map { _ match {
      case Some((folder, _)) => 
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

  // TODO show only to folder owners and collaborators?
  def getFolderCollaborators(id: UUID) = silhouette.SecuredAction.async { implicit request =>
    val f = for {
      f <- folders.getFolder(id, request.identity.username)
      policies <- folders.getCollaborators(id)
    } yield (f.map(_._1), policies)

    f.map { _ match {
      case (Some(folder), policies) => 
        if (folder.getOwner == request.identity.username)
          jsonOk(Json.obj(
            "id" -> folder.getId,
            "collaborators" -> policies.map { p => Json.obj(
              "username" -> p.getSharedWith,
              "access_level" -> p.getAccessLevel
            )}
          ))
        else 
          Forbidden

      case _ => 
        NotFound
    }}
  }

  def setFolderVisibility() = silhouette.SecuredAction.async { implicit request =>
    request.body.asJson match {
      case Some(json) => 
        val id = (json \ "ids").as[Seq[UUID]].head
        val visibility = (json \ "visibility").asOpt[String]
          .map(PublicAccess.Visibility.withName)
        val accessLevel = (json \ "access_level").asOpt[String]
          .flatMap(PublicAccess.AccessLevel.withName)

        // Using method from SetVisibilityHelper
        setVisibilityRecursive(id, request.identity.username, visibility, accessLevel)
          .map { success => 
            if (success) Ok else MultiStatus
          }
        
      case None =>
        Future.successful(BadRequest)
    }
  }

  // TODO restrict to folder owners and admins
  // TODO subfolders
  def addFolderCollaborator() = silhouette.SecuredAction.async { implicit request =>
    request.body.asJson match {
      case Some(json) => 
        val id = (json \ "ids").as[Seq[UUID]].head
        val collaborator = (json \ "username").as[String]
        val level = (json \ "access_level").asOpt[String]
          .flatMap(SharingLevel.withName)
          .getOrElse(SharingLevel.READ)
        
        // Using method from AddCollaboratorHelper
        addCollaboratorRecursive(id, request.identity.username, collaborator, level)
          .map { success => 
            if (success) Ok else MultiStatus
          }

      case None =>
        Future.successful(BadRequest)
    }
  }

  def removeFolderCollaborator() = silhouette.SecuredAction.async { implicit request => ??? 
    
  }

  // TODO set public access for (list of) document(s)

  // TODO add collaborators to document(s)

  // TODO remove collaborators from document(s)

}

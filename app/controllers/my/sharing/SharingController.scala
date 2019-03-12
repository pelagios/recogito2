package controllers.my.sharing

import controllers.{Security, HasPrettyPrintJSON}
import com.mohiva.play.silhouette.api.Silhouette
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import services.{PublicAccess, SharingLevel}
import services.folder.FolderService
import services.user.UserService

@Singleton
class SharingController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[Security.Env],
  users: UserService,
  implicit val ctx: ExecutionContext,
  implicit val folders: FolderService
) extends AbstractController(components) 
    with HasPrettyPrintJSON 
    with helpers.SetVisibilityHelper {
  
  def searchUsers(query: String) = silhouette.SecuredAction.async { implicit request =>
    users.searchUsers(query, 10).map { matches =>
      jsonOk(Json.toJson(matches))
    }
  }

  // TODO restrict to folder owners and admins
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

  // TODO restrict to folder owners and admins
  // TODO subfolders
  def setFolderVisibility() = silhouette.SecuredAction.async { implicit request =>
    request.body.asJson match {
      case Some(json) => 
        val id = (json \ "ids").as[Seq[UUID]].head
        val visibility = (json \ "visibility").asOpt[String]
          .map(PublicAccess.Visibility.withName)
        val accessLevel = (json \ "access_level").asOpt[String]
          .flatMap(PublicAccess.AccessLevel.withName)

        setVisibilityRecursive(id, request.identity.username, visibility, accessLevel)
          .map { success => 
            if (success) Ok else MultiStatus
          }
        
      case None =>
        Future.successful(BadRequest)
    }
    
    /*  
    folders.getChildrenRecursive(id).map { idsAndTitles =>
      Ok
    }
    */
  }


  // TODO restrict to folder owners and admins
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

  // TODO restrict to folder owners and admins
  // TODO subfolders
  def addFolderCollaborator() = silhouette.SecuredAction.async { implicit request =>
    request.body.asJson match {
      case Some(json) => 
        val ids = (json \ "ids").as[Seq[UUID]]
        val collaborator = (json \ "username").as[String]
        val level = (json \ "access_level").asOpt[String]
          .flatMap(SharingLevel.withName)
          .getOrElse(SharingLevel.READ)
        
        folders.addCollaborator(
          ids.head, request.identity.username, collaborator, level
        ).map { _ => Ok }

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

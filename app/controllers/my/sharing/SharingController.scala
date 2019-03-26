package controllers.my.sharing

import controllers.{Security, HasPrettyPrintJSON}
import com.mohiva.play.silhouette.api.Silhouette
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import services.{PublicAccess, SharingLevel}
import services.SharingLevel.Utils._
import services.document.DocumentService
import services.folder.FolderService
import services.generated.tables.records.{FolderRecord, SharingPolicyRecord}
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
    with helpers.RemoveCollaboratorHelper
    with helpers.SetVisibilityHelper {
  
  /** API method to search usernames. Open to any logged in user. **/
  def searchUsers(query: String) = silhouette.SecuredAction.async { implicit request =>
    users.searchUsers(query, 10).map { matches =>
      jsonOk(Json.toJson(matches))
    }
  }

  /** API method to get visibility settings on a folder. Open to anyone with 
    * admin permissions on the folder. 
    */
  def getFolderVisibility(id: UUID) = silhouette.SecuredAction.async { implicit request => 
    folders.getFolder(id, request.identity.username).map { _ match {
      case Some((folder, policy)) => 
        if (isFolderAdmin(request.identity.username, folder, policy))
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

  /** API method to get the collaborators on a folder. Open to anyone with
    * admin permissions on the folder.
    */
  def getFolderCollaborators(id: UUID) = silhouette.SecuredAction.async { implicit request =>
    val f = for {
      f <- folders.getFolder(id, request.identity.username)
      collabPolicies <- folders.getFolderCollaborators(id)
    } yield (f, collabPolicies)

    f.map { _ match {
      case (Some((folder, policy)), collabPolicies) => 
        if (isFolderAdmin(request.identity.username, folder, policy))
          jsonOk(Json.obj(
            "id" -> folder.getId,
            "collaborators" -> collabPolicies.map { p => Json.obj(
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

  /** API method to set folder visibility. Open to folder admins. **/
  def setFolderVisibility() = silhouette.SecuredAction.async { implicit request =>
    request.body.asJson match {
      case Some(json) => 
        val id = (json \ "ids").as[Seq[UUID]].head
        val visibility = (json \ "visibility").asOpt[String]
          .map(PublicAccess.Visibility.withName)
        val accessLevel = (json \ "access_level").asOpt[String]
          .flatMap(PublicAccess.AccessLevel.withName)

        // Using method from SetVisibilityHelper (also checks permissions)
        setVisibilityRecursive(id, request.identity.username, visibility, accessLevel)
          .map { success => 
            if (success) Ok else MultiStatus
          }
        
      case None =>
        Future.successful(BadRequest)
    }
  }

  /** API method to add a folder collaborator. Open to folder admins **/
  def addFolderCollaborator() = silhouette.SecuredAction.async { implicit request =>
    request.body.asJson match {
      case Some(json) => 
        val id = (json \ "ids").as[Seq[UUID]].head
        val collaborator = (json \ "username").as[String]
        val level = (json \ "access_level").asOpt[String]
          .flatMap(SharingLevel.withName)
          .getOrElse(SharingLevel.READ)
        
        // Using method from AddCollaboratorHelper (also checks permissions)
        addCollaboratorRecursive(id, request.identity.username, collaborator, level)
          .map { success => 
            if (success) Ok else MultiStatus
          }

      case None =>
        Future.successful(BadRequest)
    }
  }

  /** API methdo to remove a folder collaborator. Open to folder admins. **/  
  def removeFolderCollaborator() = silhouette.SecuredAction.async { implicit request =>
    request.body.asJson match {
      case Some(json) =>
        val id = (json \ "ids").as[Seq[UUID]].head
        val collaborator = (json \ "username").as[String]

        removeCollaboratorRecursive(id, request.identity.username, collaborator)
          .map { success => 
            if (success) Ok else MultiStatus
          }

      case None =>
        Future.successful(BadRequest)
    }
  }

  // TODO set public access for (list of) document(s)

  // TODO add collaborators to document(s)

  // TODO remove collaborators from document(s)

}

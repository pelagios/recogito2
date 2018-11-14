package controllers.my.ng.folders

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import services.folder.FolderService
import services.folder.FolderService.folderWrites
import services.user.UserService

@Singleton
class FolderController @Inject() (
    val components: ControllerComponents,
    val folders: FolderService,
    val silhouette: Silhouette[Security.Env],
    val users: UserService,
    val config: Configuration,
    implicit val ctx: ExecutionContext
  ) extends BaseController(components, config, users)
      with HasPrettyPrintJSON {

  def createFolder() = silhouette.SecuredAction.async { implicit request => 
    request.body.asJson match {
      case None => Future.successful(BadRequest)

      case Some(json) =>
        (json \ "title").asOpt[String] match {
          case None => Future.successful(BadRequest)

          case Some(title) =>
            val parent = (json \ "parent").asOpt[UUID]
            folders.createFolder(request.identity.username, title, parent).map { folder => 
              jsonOk(Json.obj("id" -> folder.getId))
            }
        }
    }
  }

  def deleteFolder(id: UUID) = silhouette.SecuredAction.async { implicit request => 
    folders.getFolder(id).flatMap { _ match {
      case Some(folder) =>
        val isOwner = folder.getOwner == request.identity.username
        if (isOwner) {
          folders.deleteFolder(folder.getId).map { success => 
            if (success) Ok
            else InternalServerError
          }
        } else {
          Future.successful(Forbidden)
        }

      case None => Future.successful(NotFound)
    }}
  }

  def bulkDeleteFolders = silhouette.SecuredAction.async { implicit request => ??? }

  def getMyFolders(offset: Int, limit: Int) = silhouette.SecuredAction.async { implicit request => 
    folders.listFolders(request.identity.username, offset, limit).map { result =>
      jsonOk(Json.toJson(result))
    }
  }

}
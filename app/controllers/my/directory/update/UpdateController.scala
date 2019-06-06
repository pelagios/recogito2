package controllers.my.directory.update

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContent, Request, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import services.SharingLevel.Utils._
import services.user.UserService
import services.folder.FolderService

@Singleton
class UpdateController @Inject() (
  val components: ControllerComponents,
  val silhouette: Silhouette[Security.Env],
  val folders: FolderService,
  val users: UserService,
  implicit val config: Configuration,
  implicit val ctx: ExecutionContext
) extends BaseController(components, config, users)
    with HasPrettyPrintJSON {

  private def renameFolder(
    id: UUID, config: JsValue
  )(implicit request: SecuredRequest[Security.Env, AnyContent]) = {

    (config \ "title").asOpt[String] match {
      case None => Future.successful(BadRequest)

      case Some(title) => 
        folders.getFolder(id, request.identity.username).flatMap { _ match {
          case Some((folder, policy)) =>
            if (isFolderAdmin(request.identity.username, folder, policy))
              folders.renameFolder(id, title).map { success =>
                if (success) Ok else InternalServerError
              }
            else 
              Future.successful(Forbidden)

          case None => Future.successful(NotFound)
        }}
    }
  }

  private def moveFolder(
    id: UUID, config: JsValue
  )(implicit request: SecuredRequest[Security.Env, AnyContent]) = ???

  private def moveDocument(
    id: String, config: JsValue
  )(implicit request: SecuredRequest[Security.Env, AnyContent]) = ???

  /** General folder update handler.
    * 
    * Currently supported update actions: RENAME, MOVE_TO
    */
  def updateFolder(id: UUID) = silhouette.SecuredAction.async { implicit request => 
    request.body.asJson match {
      case None => Future.successful(BadRequest)

      case Some(json) => 
        (json \ "action").asOpt[String] match {
          case Some("RENAME") => renameFolder(id, json)
          case Some("MOVE_TO") => moveFolder(id, json)
          case None => Future.successful(BadRequest)
        }
    }
  }

  def updateDocument(id: String) = silhouette.SecuredAction.async { implicit request => 
    request.body.asJson match {
      case None => Future.successful(BadRequest)

      case Some(json) => 
        (json \ "action").asOpt[String] match {
          case Some("MOVE_TO") => moveDocument(id, json)
          case None => Future.successful(BadRequest)
        }
    }
  }

}
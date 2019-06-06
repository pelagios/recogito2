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
import services.document.DocumentService
import services.folder.FolderService
import services.user.UserService

@Singleton
class UpdateController @Inject() (
  val components: ControllerComponents,
  val silhouette: Silhouette[Security.Env],
  val documents: DocumentService,
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

  private def moveOneFolder(id: UUID, newParentId: UUID, username: String): Future[Boolean] = {
    val f = for {
      folder <- folders.getFolder(id, username)
      parent <- folders.getFolder(newParentId, username)
    } yield (folder, parent)

    f.flatMap { case (folder, parent) => 
      val hasFolderAdminRights = folder.map(t => isFolderAdmin(username, t._1, t._2)).getOrElse(false)
      val hasParentAdminRights = parent.map(t => isFolderAdmin(username, t._1, t._2)).getOrElse(false)

      if (hasFolderAdminRights && hasParentAdminRights)
        folders.moveFolder(id, newParentId)
      else 
        Future.successful(false)
    }
  }

  /** Move one document to the given folder.
    * 
    * Requires admin rights on the document as well as the folder.
    */
  private def moveOneDocument(docId: String, folderId: UUID, username: String): Future[Boolean] = {
    val f = for {
      d <- documents.getDocumentRecordById(docId, Some(username))
      f <- folders.getFolder(folderId, username)
    } yield (d, f)

    f.flatMap { case (d, f) => 
      val hasDocAdminRights = d.map(_._2.isAdmin).getOrElse(false)
      val hasFolderAdminRights = f.map(t => isFolderAdmin(username, t._1, t._2)).getOrElse(false)

      if (hasDocAdminRights && hasFolderAdminRights)
        folders.moveDocumentToFolder(docId, folderId).map(_ => true)
      else 
        Future.successful(false)
    }
  }

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

          case Some("MOVE_TO") => 
            (json \ "destination").asOpt[UUID] match {
              case None => Future.successful(BadRequest)

              case Some(destination) => 
                moveOneFolder(id, destination, request.identity.username)
                  .map { success => if (success) Ok else BadRequest }
                  .recover { case t: Throwable => InternalServerError }
            }

          case None => Future.successful(BadRequest)
        }
    }
  }

  def updateDocument(id: String) = silhouette.SecuredAction.async { implicit request => 
    request.body.asJson match {
      case None => Future.successful(BadRequest)

      case Some(json) => 
        (json \ "action").asOpt[String] match {

          case Some("MOVE_TO") => 
            (json \ "destination").asOpt[UUID] match {
              case None => Future.successful(BadRequest)

              case Some(folderId) => 
                moveOneDocument(id, folderId, request.identity.username)
                  .map { success => if (success) Ok else BadRequest }
                  .recover { case t: Throwable => InternalServerError }
            }

          case None => Future.successful(BadRequest)
        }
    }
  }

  def bulkUpdateFolders = silhouette.SecuredAction.async { implicit request => 
    request.body.asJson match {
      case None => Future.successful(BadRequest)
      case Some(json) => 
        val action = (json \ "action").asOpt[String]
        val folderIds = (json \ "folders").asOpt[Seq[UUID]]
        val destination = (json \ "destination").asOpt[UUID]

        (action, folderIds, destination) match {
          case (Some("MOVE_TO"), Some(folderIds), Some(destination)) =>
            Future.sequence {
              folderIds.map(id => moveOneFolder(id, destination, request.identity.username))
            } map { successes =>
              if (!successes.contains(false)) Ok else BadRequest
            }

          case _ => Future.successful(BadRequest)
        }
    }
  }

  def bulkUpdateDocuments = silhouette.SecuredAction.async { implicit request => 
    request.body.asJson match {
      case None => Future.successful(BadRequest)
      case Some(json) => 
        val action = (json \ "action").asOpt[String]
        val documentIds = (json \ "documents").asOpt[Seq[String]]
        val destination = (json \ "destination").asOpt[UUID]

        (action, documentIds, destination) match {
          case (Some("MOVE_TO"), Some(docIds), Some(destination)) => 
            Future.sequence { 
              docIds.map(id => moveOneDocument(id, destination, request.identity.username))
            } map { successes => 
              if (!successes.contains(false)) Ok else BadRequest
            }

          case _ => Future.successful(BadRequest)
        }
    }
  }

}
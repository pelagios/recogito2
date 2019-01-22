package controllers.my.directory.create

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import services.ContentType
import services.folder.FolderService
import services.upload.UploadService
import services.task.TaskType
import services.user.UserService
import transform.ner.NERService
import transform.tei.TEIParserService
import transform.tiling.TilingService

@Singleton
class CreateController @Inject() (
  val components: ControllerComponents,
  val folders: FolderService,
  val silhouette: Silhouette[Security.Env],
  val uploads: UploadService,
  val users: UserService,
  val config: Configuration,
  val tilingService: TilingService,
  val teiParserService: TEIParserService,
  val nerService: NERService,
  implicit val ctx: ExecutionContext,
  implicit val ws: WSClient
) extends BaseController(components, config, users)
    with types.FileUpload
    with types.IIIFSource
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

  def renameFolder(id: UUID, title: String) = silhouette.SecuredAction.async { implicit request => 
    folders.getFolder(id).flatMap { _ match {
      case Some(folder) =>
        // For the time being, only folder owner may rename
        if (folder.getOwner == request.identity.username)
          folders.renameFolder(id, title).map { success =>
            if (success) Ok else InternalServerError
          }
        else 
          Future.successful(Forbidden)

      case None => Future.successful(NotFound)
    }}
  }

  def createReadme(folderId: UUID) = silhouette.SecuredAction.async { implicit request =>
    request.body.asJson match {
      case Some(json) =>
        val data = (json \ "data").as[String]

        val f = Option(folderId) match {
          case Some(id) => folders.setReadme(id, data)
          case None => users.setReadme(request.identity.username, data)
        }
        
        f.map { success => 
          if (success) Ok else InternalServerError
        }

      case None => Future.successful(BadRequest)
    }
  }

  /** Initializes a new upload record **/
  def initUpload() = silhouette.SecuredAction.async { implicit request =>
    val title = request.body.asMultipartFormData.flatMap(_.dataParts.get("title"))
          .getOrElse(Seq.empty[String])
          .headOption
          .getOrElse("New document")

    uploads.createPendingUpload(request.identity.username, title).map { upload =>
      jsonOk(Json.obj("id" -> upload.getId.toInt))
    }
  }

  def storeFilepart(uploadId: Int) = silhouette.SecuredAction.async { implicit request => 
    // File or remote URL?
    val isFileupload = request.body.asMultipartFormData.flatMap(_.file("file")).isDefined

    uploads.findPendingUpload(request.identity.username).flatMap { _ match {
      case None =>
        Future.successful(NotFound)

      // Only the current user can add files to this upload
      case Some(pendingUpload) if (pendingUpload.getId != uploadId) => 
        Future.successful(Forbidden)

      case Some(pendingUpload) =>
        if (isFileupload)
          storeFile(pendingUpload, request.identity, request.body)
        else 
          registerIIIFSource(pendingUpload, request.identity, request.body)
    }}
  }

  /** Finalizes the upload.
    * 
    * Bit of a mix of concerns currently. Triggers processing services (NER, tiling, etc.)
    * and converts the Upload to a Recogito Document
    */
  def finalizeUpload(id: Int, folder: Option[UUID]) = silhouette.SecuredAction.async { implicit request =>
    uploads.findPendingUploadWithFileparts(request.identity.username).flatMap(_ match {
      case None =>
        Future.successful(NotFound)

      case Some((pendingUpload, Seq())) =>
        // No fileparts: doesn't make sense - just cancel the pending upload
        uploads.deletePendingUpload(request.identity.username).map { success =>
          if (success) Ok else InternalServerError
        }
    
      case Some((pendingUpload, fileparts)) =>
        uploads.importPendingUpload(pendingUpload, fileparts, folder).map { case (doc, docParts) => {

          // TODO Change this to new task API!

          // We'll forward a list of the running processing tasks to the view, so it can show progress
          val runningTasks = scala.collection.mutable.ListBuffer.empty[TaskType]

          // Tile images
          val imageParts = docParts.filter(_.getContentType.equals(ContentType.IMAGE_UPLOAD.toString))
          if (imageParts.size > 0) {
            tilingService.spawnJob(doc, imageParts)
            runningTasks.append(TilingService.TASK_TYPE)
          }
          
          // Parse TEI
          val teiParts = docParts.filter(_.getContentType.equals(ContentType.TEXT_TEIXML.toString))
          if (teiParts.size > 0) {
            teiParserService.spawnJob(doc, teiParts)
            runningTasks.append(TEIParserService.TASK_TYPE)              
          }

          jsonOk(Json.obj(
            "document_id" -> doc.getId,
            "running_tasks" -> runningTasks.map(_.toString)
          ))
        }}
    })
  }

}
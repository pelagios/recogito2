package controllers.my.directory.create

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import services.{SharingLevel, ContentType}
import services.SharingLevel.Utils._
import services.annotation.AnnotationService
import services.document.DocumentService
import services.folder.FolderService
import services.generated.tables.records.{FolderRecord, SharingPolicyRecord}
import services.upload.UploadService
import services.task.TaskType
import services.user.UserService
import transform.ner.NERService
import transform.tei.TEIParserService
import transform.tiling.TilingService

@Singleton
class CreateController @Inject() (
  val annotations: AnnotationService,
  val components: ControllerComponents,
  val silhouette: Silhouette[Security.Env],
  val uploads: UploadService,
  val users: UserService,
  val tilingService: TilingService,
  val teiParserService: TEIParserService,
  val nerService: NERService,
  implicit val config: Configuration,
  implicit val documents: DocumentService,
  implicit val folders: FolderService,
  implicit val ctx: ExecutionContext,
  implicit val tmpFile: TemporaryFileCreator,
  implicit val ws: WSClient
) extends BaseController(components, config, users)
    with types.FileUpload
    with types.RemoteSource
    with HasPrettyPrintJSON 
    with helpers.InheritVisibilityHelper
    with helpers.InheritCollaboratorsHelper {

  def createFolder() = silhouette.SecuredAction.async { implicit request => 
    request.body.asJson match {
      case None => Future.successful(BadRequest)

      case Some(json) =>
        (json \ "title").asOpt[String] match {
          case None => Future.successful(BadRequest)

          case Some(title) =>
            val parent = (json \ "parent").asOpt[UUID]

            val f = for {
              folder <- folders.createFolder(request.identity.username, title, parent)
              visibilitySuccess <- inheritVisibility(folder, request.identity.username)
              collaboratorSuccess <- inheritCollaborators(folder, request.identity.username)
            } yield (folder, visibilitySuccess && collaboratorSuccess)

            f.map { case (folder, success) => 
              jsonOk(Json.obj("id" -> folder.getId))
            }
        }
    }
  }

  def renameFolder(id: UUID, title: String) = silhouette.SecuredAction.async { implicit request => 
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

  def createReadme(folderId: UUID) = silhouette.SecuredAction.async { implicit request =>
    request.body.asJson match {
      case Some(json) =>
        val data = (json \ "data").as[String]

        val f = Option(folderId) match {
          case Some(id) => folders.updateReadme(id, data)
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
          registerRemoteSource(pendingUpload, request.identity, request.body)
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
        val f = for {
          (doc, docParts) <- uploads.importPendingUpload(pendingUpload, fileparts, folder)
          visibilitySuccess <- inheritVisibility(doc)
          collabSuccess <- inheritCollaborators(doc)
        } yield (doc, docParts, visibilitySuccess && collabSuccess)

        f.map { case (doc, docParts, success) => {

          // TODO Change this to new task API!

          // TODO make use of success in response

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

  /** Creates a clone of the document with the given ID.
    * 
    * The operation is either 
    * -) "duplicate" - the owner of the document creates a copy in her/his own workspace
    * -) "fork" - a visiting user creates a clone of someone else's document into her/his workspace
    */
  def cloneDocument(id: String) = silhouette.SecuredAction.async { implicit request => 
    documents.getExtendedMeta(id: String, Some(request.identity.username)).flatMap { _ match { 
      case Some((doc, accesslevel)) =>
        val newOwner = 
          if (doc.owner == request.identity.username)
            None // current user is document owner - just a duplicate, no fork
          else 
            Some(request.identity) // Fork to current user workspace

        if (accesslevel.canReadAll) { // At least read access required for forking         
          val f = for {
            result <- documents.cloneDocument(doc, doc.fileparts, newOwner)
            success <- result match { 
              case Right(cloned) =>
                annotations.cloneAnnotationsTo(
                  cloned.docIdBefore,
                  cloned.docIdAfter,
                  cloned.filepartIds)

              case _ =>
                Future.successful(false)
            }
            _ <- Future { Thread.sleep(1000) } // Horrible but ES needs time to reflect the update
          } yield (success)

          f.map { success => 
            if (success) Ok else Status(409) // Conflict
          }
        } else {
          Future.successful(Forbidden)
        } 

      case None => Future.successful(NotFound)
    }}
  }

}
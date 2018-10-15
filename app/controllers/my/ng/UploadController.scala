package controllers.my.ng

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import services.{ContentType, UnsupportedContentTypeException, UnsupportedTextEncodingException}
import services.document.DocumentService
import services.task.{TaskService, TaskType}
import services.upload.{UploadService, QuotaExceededException}
import services.generated.tables.records.UploadRecord
import services.user.{User, UserService}
import services.user.Roles._
import org.webjars.play.WebJarsUtil
import play.api.{Configuration, Logger}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContent, ControllerComponents}
import play.api.libs.ws.WSClient
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Success, Failure}
import transform.ner.NERService
import transform.tei.TEIParserService
import transform.tiling.TilingService
import transform.iiif.{IIIF, IIIFParser}

case class UploadSuccess(partId: UUID, contentType: String)

@Singleton
class UploadController @Inject() (
    val components: ControllerComponents,
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService,
    val taskService: TaskService,
    val uploads: UploadService,
    val silhouette: Silhouette[Security.Env],
    val tilingService: TilingService,
    val teiParserService: TEIParserService,
    val nerService: NERService,
    implicit val webjars: WebJarsUtil,
    implicit val ws: WSClient,
    implicit val ctx: ExecutionContext,
    implicit val system: ActorSystem
  ) extends BaseAuthController(components, config, documents, users) with I18nSupport with HasPrettyPrintJSON {
  
  implicit val uploadSuccessWrites: Writes[UploadSuccess] = (
    (JsPath \ "uuid").write[UUID] and
    (JsPath \ "content_type").write[String]
  )(unlift(UploadSuccess.unapply))

  private def storeFile(pendingUpload: UploadRecord, owner: User, body: AnyContent) = {
    val MSG_ERROR = "Something went wrong while storing your file"

    body.asMultipartFormData.map(tempfile => {
      tempfile.file("file").map { f =>
        uploads.insertUploadFilepart(pendingUpload.getId, owner, f).map(_ match {
          case Right(filepart) =>
            // Upload was properly identified and stored
            Ok(Json.toJson(UploadSuccess(filepart.getId, filepart.getContentType)))

          case Left(e: UnsupportedContentTypeException) =>
            BadRequest("Unknown or unsupported file format")
            
          case Left(e: UnsupportedTextEncodingException) =>
            BadRequest("Unknown or unsupported text encoding")
            
          case Left(e: QuotaExceededException) =>
            BadRequest("Not enough space - you only have " + e.remainingSpaceKb / 1024 + " MB remaining")

          case Left(otherFailure) =>
            // For future use
            BadRequest(MSG_ERROR)
        })
      }.getOrElse({
        // POST without a file? Not possible through the UI!
        Logger.warn("Filepart POST without file attached")
        Future.successful(BadRequest(MSG_ERROR))
      })
    }).getOrElse({
      // POST without form data? Not possible through the UI!
      Logger.warn("Filepart POST without form data")
      Future.successful(BadRequest(MSG_ERROR))
    })
  }

  private def registerIIIFSource(pendingUpload: UploadRecord, owner: User, body: AnyContent) = {
    body.asFormUrlEncoded.flatMap(_.get("iiif_source").flatMap(_.headOption)) match {
      case Some(url) =>
        // Identify type of IIIF URL - image or item manifest? 
        IIIFParser.identify(url).flatMap {  
          case Success(IIIF.IMAGE_INFO) =>     
            uploads.deleteFilePartsByUploadId(pendingUpload.getId).flatMap { _ =>
              uploads.insertRemoteFilepart(pendingUpload.getId, owner.username, ContentType.IMAGE_IIIF, url)
            }.map { success =>
              if (success) Ok else InternalServerError
            }
          
          case Success(IIIF.MANIFEST) =>
            IIIFParser.fetchManifest(url).flatMap { 
              case Success(manifest) =>

                // The weirdness of IIIF canvases. In order to get a label for the images,
                // we zip the images with the label of the canvas they are on (images don't
                // seem to have labels).
                val imagesAndLabels = manifest.sequences.flatMap(_.canvases).flatMap { canvas =>
                  canvas.images.map((_, canvas.label))
                }

                val inserts = imagesAndLabels.zipWithIndex.map { case ((image, label), idx) =>
                  uploads.insertRemoteFilepart(
                    pendingUpload.getId,
                    owner.username,
                    ContentType.IMAGE_IIIF,
                    image.service,
                    Some(label.value), 
                    Some(idx + 1)) // Remember: seq no. starts at 1 (because it's used in the URI) 
                  }
                
                Future.sequence(inserts).map { result =>
                  if (result.contains(false)) InternalServerError
                  else Ok
                }
                
              // Manifest parse error
              case Failure(e) =>
                Future.successful(BadRequest(e.getMessage))                  
            }
            
          case Failure(e) =>
            Future.successful(BadRequest(e.getMessage))
        }

      case None =>
        // POST without IIIF URL? Not possible through the UI!
        Logger.warn("IIIF POST without URL")
        Future.successful(BadRequest("Something went wrong while registering IIIF image"))
    }
  }

  /** Initializes a new upload **/
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
    val isFileupload = request.body.asMultipartFormData.isDefined

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

  def finalizeUpload(id: Int) = silhouette.SecuredAction.async { implicit request =>
    uploads.findPendingUploadWithFileparts(request.identity.username).flatMap(_ match {
      case None =>
        Future.successful(NotFound)

      case Some((pendingUpload, Seq())) =>
        // No fileparts - doesn't make sense
        Future.successful(BadRequest)
    
      case Some((pendingUpload, fileparts)) =>
        uploads.importPendingUpload(pendingUpload, fileparts).map { case (doc, docParts) => {
          // We'll forward a list of the running processing tasks to the view, so it can show progress
          val runningTasks = scala.collection.mutable.ListBuffer.empty[TaskType]
          
          // TODO this bit should be cleaned up

          // Apply NER if requested
          val applyNER = checkParamValue("apply-ner", "on")
          if (applyNER) {
            nerService.spawnTask(doc, docParts)
            runningTasks.append(NERService.TASK_TYPE)
          }

          // Tile images
          val imageParts = docParts.filter(_.getContentType.equals(ContentType.IMAGE_UPLOAD.toString))
          if (imageParts.size > 0) {
            tilingService.spawnTask(doc, imageParts)
            runningTasks.append(TilingService.TASK_TYPE)
          }
          
          // Parse TEI
          val teiParts = docParts.filter(_.getContentType.equals(ContentType.TEXT_TEIXML.toString))
          if (teiParts.size > 0) {
            teiParserService.spawnTask(doc, teiParts)
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

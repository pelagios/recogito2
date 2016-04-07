package controllers.my.upload

import akka.actor.ActorSystem
import controllers.BaseController
import controllers.my.upload.ProcessingTaskMessages._
import controllers.my.upload.ner.NERService
import controllers.my.upload.tiling.TilingService
import java.io.File
import javax.inject.Inject
import models.content.ContentIdentificationFailures._
import models.content.{ ContentType, DocumentService, UploadService }
import models.generated.tables.records.UploadRecord
import models.user.Roles._
import play.api.Logger
import play.api.Play.current
import play.api.cache.CacheApi
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions
import storage.DB

case class UploadSuccess(contentType: String)

case class NewDocumentData(title: String, author: String, dateFreeform: String, description: String, source: String, language: String)

class UploadController @Inject() (implicit val cache: CacheApi, val db: DB, system: ActorSystem, context: ExecutionContext) extends BaseController {

  private val FILE_ARG = "file"

  private val MSG_ERROR = "There was an error processing your data"

  val newDocumentForm = Form(
    mapping(
      "title" -> nonEmptyText,
      "author" -> text,
      "date_freeform" -> text,
      "description" -> text,
      "source" -> text,
      "language" -> text
    )(NewDocumentData.apply)(NewDocumentData.unapply)
  )

  implicit def uploadRecordToNewDocumentData(r: UploadRecord) =
    NewDocumentData(r.getTitle, r.getAuthor, r.getDateFreeform, r.getDescription, r.getSource, r.getLanguage)


  def showStep1(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UploadService.findPendingUpload(loggedIn.user.getUsername).map(_ match {
      case Some(pendingUpload) =>
        Ok(views.html.my.upload.step1(usernameInPath, newDocumentForm.fill(pendingUpload)))

      case None =>
        Ok(views.html.my.upload.step1(usernameInPath, newDocumentForm))
    })
  }

  /** Stores document metadata following step 1 **/
  def storeDocumentMetadata(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    newDocumentForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.my.upload.step1(usernameInPath, formWithErrors))),

      docData =>
        UploadService.storePendingUpload(loggedIn.user.getUsername, docData.title, docData.author, docData.dateFreeform, docData.description, docData.source, docData.language)
          .flatMap(user => Future.successful(Redirect(controllers.my.upload.routes.UploadController.showStep2(usernameInPath))))
          .recover { case t: Throwable => {
            t.printStackTrace()
            Ok(views.html.my.upload.step1(usernameInPath, newDocumentForm.bindFromRequest, Some(MSG_ERROR)))
          }}
    )
  }

  /** Step 2 requires that a pending upload exists - otherwise, redirect to step 1 **/
  def showStep2(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UploadService.findPendingUploadWithFileparts(loggedIn.user.getUsername).map(_ match {
      case Some((pendingUpload, fileparts)) =>
        Ok(views.html.my.upload.step2(usernameInPath, fileparts))

      case None =>
        Redirect(controllers.my.upload.routes.UploadController.showStep1(usernameInPath))
    })
  }

  /** Stores a filepart during step 2 **/
  def storeFilepart(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>

    import UploadController._

    // First, we need to get the pending upload this filepart belongs to
    val username = loggedIn.user.getUsername
    UploadService.findPendingUpload(username)
      .flatMap(_ match {
        case Some(pendingUpload) =>
          request.body.asMultipartFormData.map(tempfile => {
            tempfile.file(FILE_ARG).map(f => {
              UploadService.insertFilepart(pendingUpload.getId, username, f).map(_ match {
                case Right(filepart) =>
                  // Upload was properly identified and stored
                  Ok(Json.toJson(UploadSuccess(filepart.getContentType)))

                case Left(UnsupportedContentType) =>
                  BadRequest("Unknown or unsupported file format")

                case Left(otherFailure) =>
                  // For future use
                  BadRequest(MSG_ERROR)
              })
            }).getOrElse({
              // POST without a file? Not possible through the UI!
              Logger.warn("Filepart POST without file attached")
              Future.successful(BadRequest(MSG_ERROR))
            })
          }).getOrElse({
            // POST without form data? Not possible through the UI!
            Logger.warn("Filepart POST without form data")
            Future.successful(BadRequest(MSG_ERROR))
          })

        case None => {
          // No pending upload stored in database? Not possible through the UI!
          Logger.warn("Filepart POST without pending upload")
          Future.successful(BadRequest(MSG_ERROR))
        }
      })
      .recover { case t: Throwable =>
        t.printStackTrace()
        BadRequest(MSG_ERROR)
      }
  }

  /** Deletes a filepart during step 2 **/
  def deleteFilepart(usernameInPath: String, filename: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UploadService.deleteFilepartByTitleAndOwner(filename, loggedIn.user.getUsername).map(success => {
      if (success) Ok("ok.") else NotFound
    })
  }

  /** Step 3 requires that a pending upload and at least one filepart exists - otherwise, redirect **/
  def showStep3(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UploadService.findPendingUploadWithFileparts(loggedIn.user.getUsername).flatMap(_ match {
      case Some((pendingUpload, fileparts)) =>
        if (fileparts.isEmpty) {
          // No fileparts - force user to step 2
          Future.successful(Redirect(controllers.my.upload.routes.UploadController.showStep2(usernameInPath)))
        } else {
          // Pending upload + fileparts available - proceed
          UploadService.importPendingUpload(pendingUpload, fileparts).map { case (doc, docParts) => {
            // We'll forward a list of the running processing tasks to the view, so it can show progress
            val runningTasks = scala.collection.mutable.ListBuffer.empty[TaskType]

            // Apply NER if requested
            val applyNER = checkParamValue("apply-ner", "on")
            if (applyNER) {
              NERService.spawnTask(doc, docParts)
              runningTasks.append(NERService.TASK_NER)
            }

            // Tile images
            val imageParts = docParts.filter(_.getContentType.equals(ContentType.IMAGE_UPLOAD.toString))
            if (imageParts.size > 0) {
              TilingService.spawnTask(doc, imageParts)
              runningTasks.append(TilingService.TASK_TILING)
            }

            Ok(views.html.my.upload.step3(usernameInPath, doc, docParts, runningTasks))
          }}
        }

      case None =>
        // No pending upload - force user to step 1
        Future.successful(Redirect(controllers.my.upload.routes.UploadController.showStep1(usernameInPath)))
    })
  }

  def cancelUploadWizard(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UploadService
      .deletePendingUpload(loggedIn.user.getUsername)
      .map(success => {
        // TODO add error message if success == false
        Redirect(controllers.my.routes.MyRecogitoController.index(usernameInPath))
      })
      .recover{ case t =>
        // TODO add error message
        Redirect(controllers.my.routes.MyRecogitoController.index(usernameInPath))
      }
  }

  /** Queries for processing progress on a specific task and document (user needs to be logged in and own the document) **/
  private def queryTaskProgress(username: String, docId: String, service: ProcessingService) = AsyncStack(AuthorityKey -> Normal) { implicit request =>

    import UploadController._  // Message (de)serialization

    DocumentService.findById(docId).flatMap(_ match {
      case Some(document) if document.getOwner == loggedIn.user.getUsername => {
        service.queryProgress(docId).map(_ match {
          case Some(result) =>
            Ok(Json.toJson(result))
          case None =>
            NotFound
        })
      }

      case Some(document) =>
        // Document exists, but belongs to another user
        Future.successful(Forbidden)

      case None =>
        Future.successful(NotFound)
    })
  }

  def queryNERProgress(usernameInPath: String, id: String) = queryTaskProgress(usernameInPath, id, NERService)

  def queryTilingProgress(usernameInPath: String, id: String) = queryTaskProgress(usernameInPath, id, TilingService)

}

/** Defines JSON serialization for NER progress messages **/
object UploadController {

  implicit val uploadSuccessWrites: Writes[UploadSuccess] =
    (JsPath \ "content_type").write[String].contramap(_.contentType)

  implicit val progressStatusValueWrites: Writes[ProgressStatus.Value] =
    Writes[ProgressStatus.Value](status => JsString(status.toString))

  implicit val taskTypeWrites: Writes[TaskType] =
    Writes[TaskType](t => JsString(t.name))

  implicit val workerProgressWrites: Writes[WorkerProgress] = (
    (JsPath \ "filepart_id").write[Int] and
    (JsPath \ "status").write[ProgressStatus.Value] and
    (JsPath \ "progress").write[Double]
  )(unlift(WorkerProgress.unapply))

  implicit val documentProgressWrites: Writes[DocumentProgress] = (
    (JsPath \ "document_id").write[String] and
    (JsPath \ "task_name").write[TaskType] and
    (JsPath \ "progress").write[Seq[WorkerProgress]]
  )(unlift(DocumentProgress.unapply))

}

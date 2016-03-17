package controllers.my.upload

import akka.actor.ActorSystem
import controllers.{ AbstractController, Security }
import controllers.my.upload.Messages._
import controllers.my.upload.ner.NERService
import controllers.my.upload.tiling.TilingService
import java.io.File
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.content.ContentAnalysisFailures._
import models.content.{ ContentType, DocumentService, UploadService }
import models.generated.tables.records.UploadRecord
import models.user.Roles._
import play.api.Logger
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.language.implicitConversions
import storage.DB

case class NewDocumentData(title: String, author: String, dateFreeform: String, description: String, source: String, language: String)

class UploadController @Inject() (implicit val db: DB, system: ActorSystem) extends AbstractController with AuthElement with Security {

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
    UploadService.findPendingUpload(loggedIn.getUsername).map(_ match {
      case Some(pendingUpload) =>
        Ok(views.html.my.upload.upload_1(usernameInPath, newDocumentForm.fill(pendingUpload)))

      case None =>
        Ok(views.html.my.upload.upload_1(usernameInPath, newDocumentForm))
    })
  }


  /** Stores document metadata following step 1 **/
  def storeDocumentMetadata(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    newDocumentForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.my.upload.upload_1(usernameInPath, formWithErrors))),

      docData =>
        UploadService.storePendingUpload(loggedIn.getUsername, docData.title, docData.author, docData.dateFreeform, docData.description, docData.source, docData.language)
          .flatMap(user => Future.successful(Redirect(controllers.my.upload.routes.UploadController.showStep2(usernameInPath))))
          .recover { case t: Throwable => {
            t.printStackTrace()
            Ok(views.html.my.upload.upload_1(usernameInPath, newDocumentForm.bindFromRequest, Some(MSG_ERROR)))
          }}
    )
  }


  /** Step 2 requires that a pending upload exists - otherwise, redirect to step 1 **/
  def showStep2(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UploadService.findPendingUploadWithFileparts(loggedIn.getUsername).map(_ match {
      case Some((pendingUpload, fileparts)) =>
        Ok(views.html.my.upload.upload_2(usernameInPath, fileparts))

      case None =>
        Redirect(controllers.my.upload.routes.UploadController.showStep1(usernameInPath))
    })
  }


  /** Stores a filepart during step 2 **/
  def storeFilepart(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    // First, we need to get the pending upload this filepart belongs to
    val username = loggedIn.getUsername
    UploadService.findPendingUpload(username)
      .flatMap(_ match {
        case Some(pendingUpload) =>
          request.body.asMultipartFormData.map(tempfile => {
            tempfile.file(FILE_ARG).map(f => {
              UploadService.insertFilepart(pendingUpload.getId, username, f).map(_ match {
                case Right(filepart) =>
                  // Upload was properly identified and stored
                  Status(OK)
                  
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
      .recover { case t: Throwable => {
        t.printStackTrace()
        BadRequest(MSG_ERROR)
      }}
  }


  /** Deletes a filepart during step 2 **/
  def deleteFilepart(usernameInPath: String, filename: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UploadService.deleteFilepartByTitleAndOwner(filename, loggedIn.getUsername).map(success => {
      if (success) Ok("ok.") else NotFound
    })
  }


  /** Step 3 requires that a pending upload and at least one filepart exists - otherwise, redirect **/
  def showStep3(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UploadService.findPendingUploadWithFileparts(loggedIn.getUsername).flatMap(_ match {
      case Some((pendingUpload, fileparts)) =>
        if (fileparts.isEmpty) {
          // No fileparts - force user to step 2
          Future.successful(Redirect(controllers.my.upload.routes.UploadController.showStep2(usernameInPath)))
        } else {
          // Pending upload + fileparts available - proceed
          UploadService.importPendingUpload(pendingUpload, fileparts).map { case (doc, docParts) => {
            
            // Apply NER if requested
            val applyNER = checkParamValue("apply-ner", "on")
            if (applyNER)
              NERService.spawnNERProcess(doc, docParts)
              
            // Tile images
            val imageParts = docParts.filter(_.getContentType.equals(ContentType.IMAGE_UPLOAD.toString))
            if (imageParts.size > 0)
              TilingService.spawnTilingProcess(doc, imageParts)

            Ok(views.html.my.upload.upload_3(usernameInPath, doc, docParts, applyNER))
          }}
        }

      case None =>
        // No pending upload - force user to step 1
        Future.successful(Redirect(controllers.my.upload.routes.UploadController.showStep1(usernameInPath)))
    })
  }


  /** Queries the NER for progress on a document (user needs to be logged in and own the document) **/
  def queryNERProgress(usernameInPath: String, id: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>

    import UploadController._  // Message (de)serialization

    DocumentService.findById(id).flatMap(_ match {
      case Some(document) if document.getOwner == loggedIn.getUsername => {
        NERService.queryProgress(id).map(_ match {
          case Some(result) =>
            Ok(Json.toJson(result))

          case None =>
            // Document exists, but there's no NER process for it
            NotFound
        })
      }

      case Some(document) =>
        // Document exists, but belongs to another userimport controllers.AbstractController
        Future.successful(Forbidden)

      case None =>
        // Document not in database
        Future.successful(NotFound)
    })
  }


  def cancelUploadWizard(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UploadService
      .deletePendingUpload(loggedIn.getUsername)
      .map(success => {
        // TODO add error message if success == false
        Redirect(controllers.my.routes.MyRecogitoController.index(usernameInPath))
      })
      .recover{ case t =>
        // TODO add error message
        Redirect(controllers.my.routes.MyRecogitoController.index(usernameInPath))
      }
  }

}

/** Defines JSON serialization for NER progress messages **/
object UploadController {
  
  implicit val progressStatusValueWrites: Writes[ProgressStatus.Value] =
    Writes[ProgressStatus.Value](status => JsString(status.toString))
  
  implicit val workerProgressWrites: Writes[WorkerProgress] = (
    (JsPath \ "filepart_id").write[Int] and
    (JsPath \ "status").write[ProgressStatus.Value] and
    (JsPath \ "progress").write[Double]
  )(unlift(WorkerProgress.unapply))

  implicit val documentProgressWrites: Writes[DocumentProgress] = (
    (JsPath \ "document_id").write[String] and
    (JsPath \ "progress").write[Seq[WorkerProgress]]
  )(unlift(DocumentProgress.unapply))

}

package controllers.my

import akka.actor.ActorSystem
import controllers.{ BaseAuthController, HasPrettyPrintJSON, WebJarAssets }
import javax.inject.{ Inject, Singleton }
import models.{ ContentType, UnsupportedContentTypeException, UnsupportedTextEncodingException }
import models.document.DocumentService
import models.task.{ TaskService, TaskType }
import models.upload.{ UploadService, QuotaExceededException }
import models.generated.tables.records.UploadRecord
import models.user.{ User, UserService }
import models.user.Roles._
import play.api.{ Configuration, Logger }
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions
import transform.ner.NERService
import transform.tei.TEIParserService
import transform.tiling.TilingService

case class UploadSuccess(contentType: String)

case class NewDocumentData(title: String, author: String, dateFreeform: String, description: String, language: String, source: String, edition: String)

@Singleton
class UploadController @Inject() (
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService,
    val taskService: TaskService,
    val uploads: UploadService,
    val tilingService: TilingService,
    val teiParserService: TEIParserService,
    val nerService: NERService,
    val messagesApi: MessagesApi,
    implicit val webjars: WebJarAssets,
    implicit val ctx: ExecutionContext,
    implicit val system: ActorSystem
  ) extends BaseAuthController(config, documents, users) with I18nSupport with HasPrettyPrintJSON {
  
  implicit val uploadSuccessWrites: Writes[UploadSuccess] =
    (JsPath \ "content_type").write[String].contramap(_.contentType)

  private val MSG_ERROR = "There was an error processing your data"

  val newDocumentForm = Form(
    mapping(
      "title" -> nonEmptyText,
      "author" -> text,
      "date_freeform" -> text,
      "description" -> text,
      "language" -> text.verifying("2- or 3-digit ISO language code required", { t => t.isEmpty || (t.size > 1 && t.size < 4) }),
      "source" -> text,
      "edition" -> text
    )(NewDocumentData.apply)(NewDocumentData.unapply)
  )

  implicit def uploadRecordToNewDocumentData(r: UploadRecord) =
    NewDocumentData(r.getTitle, r.getAuthor, r.getDateFreeform, r.getDescription, r.getLanguage, r.getSource, r.getEdition)

  def showStep1(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    uploads.findPendingUpload(loggedIn.username).map(_ match {
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
        uploads.storePendingUpload(loggedIn.username, docData.title, docData.author, docData.dateFreeform, docData.description, docData.language, docData.source, docData.edition)
          .flatMap(user => Future.successful(Redirect(controllers.my.routes.UploadController.showStep2(usernameInPath))))
          .recover { case t: Throwable => {
            t.printStackTrace()
            Ok(views.html.my.upload.step1(usernameInPath, newDocumentForm.bindFromRequest, Some(MSG_ERROR)))
          }}
    )
  }

  /** Step 2 requires that a pending upload exists - otherwise, redirect to step 1 **/
  def showStep2(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    uploads.findPendingUploadWithFileparts(loggedIn.username).map(_ match {
      case Some((pendingUpload, fileparts)) =>
        Ok(views.html.my.upload.step2(usernameInPath, fileparts))

      case None =>
        Redirect(controllers.my.routes.UploadController.showStep1(usernameInPath))
    })
  }

  /** Stores a filepart during step 2 **/
  def storeFilepart(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    val username = loggedIn.username
    val isFileupload = request.body.asMultipartFormData.isDefined

    def storeFilepart(pendingUpload: UploadRecord) = request.body.asMultipartFormData.map(tempfile => {
      tempfile.file("file").map(f => {
        uploads.insertUploadFilepart(pendingUpload.getId, loggedIn, f).map(_ match {
          case Right(filepart) =>
            // Upload was properly identified and stored
            Ok(Json.toJson(UploadSuccess(filepart.getContentType)))

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

    def registerIIIFSource(pendingUpload: UploadRecord) =
      request.body.asFormUrlEncoded.flatMap(_.get("iiif_source").map(_.headOption)).flatten match {
        case Some(url) =>
          uploads.insertRemoteFilepart(pendingUpload.getId, username, ContentType.IMAGE_IIIF, url).map(success =>
            if (success) Ok else InternalServerError)

        case None =>
          // POST without IIIF URL? Not possible through the UI!
          Logger.warn("IIIF POST without URL")
          Future.successful(BadRequest(MSG_ERROR))
      }

    uploads.findPendingUpload(username)
      .flatMap(_ match {
        case Some(pendingUpload) =>
          if (isFileupload) storeFilepart(pendingUpload) else registerIIIFSource(pendingUpload)

        case None =>
          // No pending upload stored in database? Not possible through the UI!
          Logger.warn("Filepart POST without pending upload")
          Future.successful(BadRequest(MSG_ERROR))
      })
      .recover { case t: Throwable =>
        t.printStackTrace()
        BadRequest(MSG_ERROR)
      }
  }

  /** Deletes a filepart during step 2 **/
  def deleteFilepart(usernameInPath: String, filename: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    uploads.deleteFilepartByTitleAndOwner(filename, loggedIn.username).map(success => {
      if (success) Ok else NotFoundPage
    })
  }

  /** Step 3 requires that a pending upload and at least one filepart exists - otherwise, redirect **/
  def showStep3(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    uploads.findPendingUploadWithFileparts(loggedIn.username).flatMap(_ match {
      case Some((pendingUpload, fileparts)) =>
        if (fileparts.isEmpty) {
          // No fileparts - force user to step 2
          Future.successful(Redirect(controllers.my.routes.UploadController.showStep2(usernameInPath)))
        } else {
          // Pending upload + fileparts available - proceed
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

            Ok(views.html.my.upload.step3(usernameInPath, doc, docParts, runningTasks))
          }}
        }

      case None =>
        // No pending upload - force user to step 1
        Future.successful(Redirect(controllers.my.routes.UploadController.showStep1(usernameInPath)))
    })
  }

  def cancelUploadWizard(usernameInPath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    uploads
      .deletePendingUpload(loggedIn.username)
      .map(success => {
        // TODO add error message if success == false
        Redirect(controllers.my.routes.MyRecogitoController.index(usernameInPath, None, None, None, None, None))
      })
      .recover{ case t =>
        // TODO add error message
        Redirect(controllers.my.routes.MyRecogitoController.index(usernameInPath, None, None, None, None, None))
      }
  }

  /** Queries for processing progress on a specific task and document (user needs to be logged in and own the document) **/
  def queryTaskProgress(username: String, docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documents.getDocumentRecord(docId, Some(loggedIn.username)).flatMap(_ match {
      // Make sure only users with read access can see the progress
      case Some((document, accesslevel)) if accesslevel.canRead => {
        taskService.findByDocument(docId).map(_ match {
          case Some(result) => jsonOk(Json.toJson(result))
          case None => NotFoundPage
        })
      }

      case Some(document) =>
        // Document exists, but no read permission
        Future.successful(ForbiddenPage)

      case None =>
        Future.successful(NotFoundPage)
    })
  }

}

package controllers.myrecogito.upload

import akka.actor.ActorSystem
import controllers.{ AbstractController, Security }
import database.DB
import java.io.File
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.Roles._
import models.UploadService
import models.generated.tables.records.UploadRecord
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

case class NewDocumentData(title: String, author: String, dateFreeform: String, description: String, source: String, language: String)

class UploadController @Inject() (implicit val db: DB, system: ActorSystem) extends AbstractController with AuthElement with Security {
  
  private val ERROR = "There was an error processing your data"

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
  
  def showStep1 = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UploadService.findForUser(loggedIn.getUsername).map(_ match {
      case Some(pendingUpload) =>
        Ok(views.html.myrecogito.upload.upload_1(newDocumentForm.fill(pendingUpload)))
        
      case None =>
        Ok(views.html.myrecogito.upload.upload_1(newDocumentForm))
    })
  }
  
  def processStep1 = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    newDocumentForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.myrecogito.upload.upload_1(formWithErrors))),
        
      docData =>
        UploadService.insertOrReplaceUpload(loggedIn.getUsername, docData.title, docData.author, docData.dateFreeform, docData.description, docData.source, docData.language)
          .flatMap(user => Future.successful(Redirect(controllers.myrecogito.upload.routes.UploadController.showStep2)))
          .recover { case t:Throwable => {
            t.printStackTrace()
            Ok(views.html.myrecogito.upload.upload_1(newDocumentForm.bindFromRequest, Some(ERROR))) 
          }}
    )
  }

  /** Step 2 requires that a pending upload exists - otherwise, redirect to step 1 **/
  def showStep2 = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UploadService.findForUser(loggedIn.getUsername).map(_ match {
      case Some(pendingUpload) =>
        Ok(views.html.myrecogito.upload.upload_2())
        
      case None =>
        Redirect(controllers.myrecogito.upload.routes.UploadController.showStep1)
    })
  }
  
  def processStep2 = StackAction(AuthorityKey -> Normal) { implicit request =>
    request.body.asMultipartFormData.map(tempfile =>
      tempfile.file("file").map { f =>
        val filename = f.filename
        // f.ref.moveTo(new File(s"$filename"))
        Ok("File uploaded")
      }.getOrElse {
        BadRequest("").flashing("error" -> "Missing file")
      }
    ).get
  }

  def showStep3 = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok(views.html.myrecogito.upload.upload_3())
  }

  def processContentUpload = AsyncStack(AuthorityKey -> Normal) {  implicit request =>
    request.body.asMultipartFormData match {

      case Some(formData) => Future.successful {
        formData.file("file") match {
          case Some(filePart) => {
              new GeoParser().parseAsync(filePart.ref.file)
              Ok("")
            }
          case None =>
            BadRequest("Form data missing")
        }
      }

      case None =>
        Future.successful(BadRequest("Form data missing"))

    }
  }

}

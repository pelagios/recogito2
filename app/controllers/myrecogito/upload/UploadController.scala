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
import play.api.Logger

case class NewDocumentData(title: String, author: String, dateFreeform: String, description: String, source: String, language: String)

class UploadController @Inject() (implicit val db: DB, system: ActorSystem) extends AbstractController with AuthElement with Security {
  
  private val FILE_ARG = "file"
  
  private val MSG_OK = "Ok."
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
  
  def showStep1 = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    UploadService.findForUser(loggedIn.getUsername).map(_ match {
      case Some(pendingUpload) =>
        Ok(views.html.myrecogito.upload.upload_1(newDocumentForm.fill(pendingUpload)))
        
      case None =>
        Ok(views.html.myrecogito.upload.upload_1(newDocumentForm))
    })
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
  
  /** Step 2 requires that a pending request and at least one filepart exists - otherwise, redirect **/
  def showStep3 = StackAction(AuthorityKey -> Normal) { implicit request =>
    /*
    val result = UploadService.findUploadForUser(loggedIn.getUsername).flatMap(_ match {
      case Some(pendingUpload) =>
        UploadService.findFilepartsByUpload(pendingUpload.getId).map(fileparts => {
          if (fileparts.isEmpty) {
            // No filepart - force user to step 2
            Redirect(controllers.myrecogito.upload.routes.UploadController.showStep2)
          } else {
            // Pending upload + at least one filepart available - proceed
            Ok("") // TODO
          }
        })
      
      case None =>
        // No pending upload - force user to step 1
        Future.successful(Redirect(controllers.myrecogito.upload.routes.UploadController.showStep1))
    })
    */
    
    Ok(views.html.myrecogito.upload.upload_3())
  }
  
  /** Stores document metadata, during step 1 **/
  def storeDocumentMetadata = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    newDocumentForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.myrecogito.upload.upload_1(formWithErrors))),
        
      docData =>
        UploadService.storeUpload(loggedIn.getUsername, docData.title, docData.author, docData.dateFreeform, docData.description, docData.source, docData.language)
          .flatMap(user => Future.successful(Redirect(controllers.myrecogito.upload.routes.UploadController.showStep2)))
          .recover { case t: Throwable => {
            t.printStackTrace()
            Ok(views.html.myrecogito.upload.upload_1(newDocumentForm.bindFromRequest, Some(MSG_ERROR))) 
          }}
    )
  }
  
  /** Stores a filepart, during step 2 **/
  def storeFilepart = AsyncStack(AuthorityKey -> Normal) { implicit request =>    
    // First, we need to get the pending upload this filepart belongs to
    UploadService.findForUser(loggedIn.getUsername)
      .flatMap(_ match {
        case Some(pendingUpload) =>
          request.body.asMultipartFormData.map(tempfile => {
            tempfile.file(FILE_ARG).map(f => {
              UploadService.insertFilepart(pendingUpload.getId, f)
                .map(_ => Ok(MSG_OK)) 
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
 
  /** Deletes a filepart, during step 2 **/
  def deleteFilepart = StackAction(AuthorityKey -> Normal) { implicit request =>
    Ok("")
  }

}

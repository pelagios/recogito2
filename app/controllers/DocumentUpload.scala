package controllers

import database.DB
import javax.inject.Inject
import play.api.mvc.{ Action, Controller }
import jp.t2v.lab.play2.auth.LoginLogout
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import jp.t2v.lab.play2.auth.AuthElement
import play.api.Play.current
import scala.concurrent.Future
import models.Documents
import models.Roles._


case class DocMetaFormData(title: String, author: String, description: String, language: String)

class DocumentUpload @Inject() (implicit val db: DB) extends AbstractController with AuthElement with Security {
  
  val metadataForm = Form(
    mapping(
      "title" -> nonEmptyText,
      "author" -> nonEmptyText,
      "description" -> nonEmptyText,
      "language" -> nonEmptyText
    )(DocMetaFormData.apply)(DocMetaFormData.unapply)
  )
  
  def showMetadataForm = StackAction (AuthorityKey ->Normal) { implicit request =>
    Ok(views.html.documentupload(metadataForm))
  }
   
  def processMetadataUpload =  AsyncStack(AuthorityKey ->Normal) {  implicit request =>
    metadataForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(views.html.documentupload(formWithErrors))),

      docMetadata =>
        Documents.insertMetadata(loggedIn.getUsername, docMetadata.title, docMetadata.author, docMetadata.description, docMetadata.language).flatMap(doc =>
           Future(Redirect(routes.DocumentUpload.showMetadataForm())) 
        )
    )
  }
  
}
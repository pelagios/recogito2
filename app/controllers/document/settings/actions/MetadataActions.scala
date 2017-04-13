package controllers.document.settings.actions

import controllers.document.settings.SettingsController
import java.util.UUID
import models.document.PartOrdering
import models.user.Roles._
import models.generated.tables.records.DocumentRecord
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.concurrent.Future

case class DocumentMetadata(

  title: String,

  author: Option[String],

  dateFreeform: Option[String],

  description: Option[String],

  language: Option[String],

  source: Option[String],

  edition: Option[String],

  license: Option[String]

)

case class FilepartMetadata(

  title: String,
  
  source: Option[String]

)

trait MetadataActions { self: SettingsController =>

  implicit val orderingReads: Reads[PartOrdering] = (
    (JsPath \ "id").read[UUID] and
    (JsPath \ "sequence_no").read[Int]
  )(PartOrdering.apply _)

  /** Sets the part sort order **/
  def setSortOrder(docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    jsonDocumentAdminAction[Seq[PartOrdering]](docId, loggedIn.user.getUsername, { case (document, ordering) =>
      documents.setFilepartSortOrder(docId, ordering).map(_ => Status(200))
    })
  }

  val documentMetadataForm = Form(
    mapping(
      "title" -> nonEmptyText,
      "author" -> optional(text),
      "date_freeform" -> optional(text),
      "description" -> optional(text(maxLength=1024)),
      "language" -> optional(text.verifying("2- or 3-digit ISO language code required", { t => t.size > 1 && t.size < 4 })),
      "source" -> optional(text),
      "edition" -> optional(text),
      "license" -> optional(text)
    )(DocumentMetadata.apply)(DocumentMetadata.unapply)
  )

  protected def metadataForm(doc: DocumentRecord) = {
    documentMetadataForm.fill(DocumentMetadata(
      doc.getTitle,
      Option(doc.getAuthor),
      Option(doc.getDateFreeform),
      Option(doc.getDescription),
      Option(doc.getLanguage),
      Option(doc.getSource),
      Option(doc.getEdition),
      Option(doc.getLicense)))

  }

  def updateDocumentMetadata(docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentAdminAction(docId, loggedIn.user.getUsername, { doc =>
      documentMetadataForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.document.settings.metadata(formWithErrors, doc, loggedIn.user))),
        
        f =>
          documents.updateMetadata(
            docId, f.title, f.author, f.dateFreeform, f.description, f.language, f.source, f.edition, f.license
          ).map { success => 
           if (success)
              Redirect(controllers.document.settings.routes.SettingsController.showDocumentSettings(docId, Some("metadata")))
                .flashing("success" -> "Your settings have been saved.")
            else 
              Redirect(controllers.document.settings.routes.SettingsController.showDocumentSettings(docId, Some("metadata")))
                .flashing("error" -> "There was an error while saving your settings.")
          }.recover { case t:Throwable =>
            t.printStackTrace()
            Redirect(controllers.document.settings.routes.SettingsController.showDocumentSettings(docId, Some("metadata")))
              .flashing("error" -> "There was an error while saving your settings.")
          }
      )
    })
  }
  
  def updateFilepartMetadata(docId: String, partId: UUID) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    
    def bindFromRequest(): Either[String, FilepartMetadata] =
      getFormParam("title") match {
        case Some(title) if title.isEmpty => Left("Title required")
        case Some(title) => Right(FilepartMetadata(title, getFormParam("source")))
        case None => Left("Title required")
      }
  
    documentAdminAction(docId, loggedIn.user.getUsername, { doc =>
      // Make sure we're not updating a part that isn't in this document
      if (doc.fileparts.exists(_.getId == partId)) {
        bindFromRequest() match {
          case Right(partMetadata) => 
            documents.updateFilepartMetadata(doc.id, partId, partMetadata.title, partMetadata.source).map { success =>
              if (success)
                Ok
              else
                InternalServerError
            }
            
          case Left(error) =>
            Future.successful(BadRequest(error))
        }       
      } else {
        Future.successful(BadRequest)
      }
    })
  }

}

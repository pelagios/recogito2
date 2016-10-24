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
      "title" -> text,
      "author" -> optional(text),
      "date_freeform" -> optional(text),
      "description" -> optional(text(maxLength=256)),
      "language" -> optional(text),
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
          Future.successful(BadRequest(views.html.document.settings.metadata(metadataForm(doc.document), doc, loggedIn.user))),

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

}

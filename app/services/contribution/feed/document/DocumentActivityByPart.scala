package services.contribution.feed.document

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContent, Request}
import services.document.ExtendedDocumentMetadata

case class DocumentActivityByPart(
  url: String, 
  partTitle: String,
  partSequenceNumber: Int,
  count: Long, 
  entries: Seq[DocumentActivityFeedEntry])

object DocumentActivityByPart {

  def build(
    partId: UUID, 
    count: Long, 
    entries: Seq[DocumentActivityFeedEntry], 
    doc: ExtendedDocumentMetadata
  )(implicit request: Request[AnyContent]) = {
    val part = doc.fileparts.find(_.getId == partId).get
    val url = controllers.document.annotation.routes.AnnotationController.showAnnotationView(doc.id, part.getSequenceNo).absoluteURL
    DocumentActivityByPart(url, part.getTitle, part.getSequenceNo, count, entries)
  }

  implicit val documentActivityByPartWrites: Writes[DocumentActivityByPart] = (
    (JsPath \ "url").write[String] and
    (JsPath \ "title").write[String] and
    (JsPath \ "sequence_number").write[Int] and
    (JsPath \ "contributions").write[Long] and 
    (JsPath \ "entries").write[Seq[DocumentActivityFeedEntry]]
  )(unlift(DocumentActivityByPart.unapply))

}

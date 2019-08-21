package services.contribution.feed.user

import play.api.mvc.{AnyContent, Request}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.generated.tables.records.DocumentRecord

trait UserActivityPerDocument {

  def documentId: String 

  def count: Long

  def enrich(doc: DocumentRecord)(implicit request: Request[AnyContent]): UserActivityPerDocument

}

object UserActivityPerDocument {

  implicit val userActivityPerDocumentWrites: Writes[UserActivityPerDocument] = (
    (JsPath \ "url").write[String] and
    (JsPath \ "title").write[String] and 
    (JsPath \ "author").writeNullable[String] and
    (JsPath \ "owner").write[String] and 
    (JsPath \ "contributions").write[Long] and
    (JsPath \ "entries").write[Seq[UserActivityFeedEntry]]
  )(_ match {
    case a: EnrichedUserActivityPerDocument => 
      (a.url, a.title, a.author, a.owner, a.count, a.entries)

    case _ =>
      // Should never happen
      throw new Exception("JSON serialization not available")
  })

}

case class RawUserActivityPerDocument(
  documentId: String,
  count: Long,
  entries: Seq[UserActivityFeedEntry]
) extends UserActivityPerDocument {

  def enrich(doc: DocumentRecord)(implicit request: Request[AnyContent]) = 
    EnrichedUserActivityPerDocument.build(documentId, count, entries, doc)

}

case class EnrichedUserActivityPerDocument(
  documentId: String,
  url: String, 
  title: String,
  author: Option[String],
  owner: String,
  count: Long, 
  entries: Seq[UserActivityFeedEntry]
) extends UserActivityPerDocument {

  // Nothing to enrich - dummy method
  def enrich(doc: DocumentRecord)(implicit request: Request[AnyContent]) = this

}

object EnrichedUserActivityPerDocument {

  def build(
    docId: String, 
    count: Long, 
    entries: Seq[UserActivityFeedEntry], 
    doc: DocumentRecord
  )(implicit request: Request[AnyContent]) =
    EnrichedUserActivityPerDocument(
      docId,
      controllers.document.routes.DocumentController.initialDocumentView(docId).absoluteURL,
      doc.getTitle,
      Option(doc.getAuthor),
      doc.getOwner,
      count,
      entries)

}

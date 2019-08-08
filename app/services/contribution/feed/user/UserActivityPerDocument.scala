package services.contribution.feed.user

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class UserActivityPerDocument(documentId: String, count: Long, entries: Seq[UserActivityFeedEntry])

object UserActivityPerDocument {

  implicit val userActivityPerDocumentWrites: Writes[UserActivityPerDocument] = (
    (JsPath \ "document_id").write[String] and
    (JsPath \ "contributions").write[Long] and 
    (JsPath \ "entries").write[Seq[UserActivityFeedEntry]]
  )(unlift(UserActivityPerDocument.unapply))

}
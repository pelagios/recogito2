package services.contribution.feed.document

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class DocumentActivityByUser(username: String, count: Long, parts: Seq[DocumentActivityByPart])

object DocumentActivityByUser {

  implicit val documentActivityByUserWrites: Writes[DocumentActivityByUser] = (
    (JsPath \ "username").write[String] and
    (JsPath \ "contributions").write[Long] and 
    (JsPath \ "parts").write[Seq[DocumentActivityByPart]]
  )(unlift(DocumentActivityByUser.unapply))

}

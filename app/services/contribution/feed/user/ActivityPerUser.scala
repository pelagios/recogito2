package services.contribution.feed.user

import play.api.mvc.{AnyContent, Request}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.generated.tables.records.DocumentRecord

case class ActivityPerUser(username: String, count: Long, documents: Seq[UserActivityPerDocument]) {

  def enrich(docs: Seq[DocumentRecord])(implicit request: Request[AnyContent]) =
    ActivityPerUser(username, count, documents.map { activity => 
      val document = docs.find(_.getId == activity.documentId).get
      activity.enrich(document)
    })

}

object ActivityPerUser {

  implicit val activityPerUserWrites: Writes[ActivityPerUser] = (
    (JsPath \ "username").write[String] and
    (JsPath \ "contributions").write[Long] and 
    (JsPath \ "documents").write[Seq[UserActivityPerDocument]]
  )(unlift(ActivityPerUser.unapply))

}
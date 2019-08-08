package services.contribution.feed.document

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasDate

case class DocumentDayActivity(timestamp: DateTime, count: Long, users: Seq[DocumentActivityByUser])

object DocumentDayActivity extends HasDate {

  implicit val documentActivityByUserWrites: Writes[DocumentDayActivity] = (
    (JsPath \ "timestamp").write[DateTime] and
    (JsPath \ "contributions").write[Long] and 
    (JsPath \ "users").write[Seq[DocumentActivityByUser]]
  )(unlift(DocumentDayActivity.unapply))

}
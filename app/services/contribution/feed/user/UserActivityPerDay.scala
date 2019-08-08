package services.contribution.feed.user

import org.joda.time.DateTime
import play.api.mvc.{AnyContent, Request}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasDate
import services.generated.tables.records.DocumentRecord

case class UserActivityPerDay(timestamp: DateTime, count: Long, users: Seq[ActivityPerUser]) {

  /** Trickles down the doc metadata into the 'by document' section **/
  def enrich(docs: Seq[DocumentRecord])(implicit request: Request[AnyContent]) = 
    UserActivityPerDay(timestamp, count, users.map(_.enrich(docs)))
    
}

object UserActivityPerDay extends HasDate {

  implicit val userActivityPerDayWrites: Writes[UserActivityPerDay] = (
    (JsPath \ "timestamp").write[DateTime] and
    (JsPath \ "contributions").write[Long] and
    (JsPath \ "users").write[Seq[ActivityPerUser]]
  )(unlift(UserActivityPerDay.unapply))

}
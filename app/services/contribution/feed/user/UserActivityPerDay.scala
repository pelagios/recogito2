package services.contribution.feed.user

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasDate

case class UserActivityPerDay(timestamp: DateTime, count: Long, users: Seq[ActivityPerUser])

object UserActivityPerDay extends HasDate {

  implicit val userActivityPerDayWrites: Writes[UserActivityPerDay] = (
    (JsPath \ "timestamp").write[DateTime] and
    (JsPath \ "contributions").write[Long] and
    (JsPath \ "users").write[Seq[ActivityPerUser]]
  )(unlift(UserActivityPerDay.unapply))

}
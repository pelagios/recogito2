package services.contribution.feed.user

import org.joda.time.DateTime
import play.api.mvc.{AnyContent, Request}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.{HasDate, RuntimeAccessLevel}
import services.generated.tables.records.DocumentRecord

case class UserActivityPerDay(timestamp: DateTime, count: Long, users: Seq[ActivityPerUser]) {

  def filter(docsAndPermissions: Seq[(DocumentRecord, RuntimeAccessLevel)]) = {
    val filteredUsers = users.map(_.filter(docsAndPermissions))
    val filteredCount = filteredUsers.map(_.count).sum
    UserActivityPerDay(timestamp, filteredCount, filteredUsers)
  }
    
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
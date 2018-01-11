package services.contribution

import org.joda.time.DateTime
import services.HasDate
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class ContributionStats(
    
  took: Long,
    
  totalContributions: Long,
  
  byUser: Seq[(String, Long)],
  
  byAction: Seq[(ContributionAction.Value, Long)],
  
  byItemType: Seq[(ItemType.Value, Long)],
  
  contributionHistory: Seq[(DateTime, Long)]
  
)

object ContributionStats extends HasDate {
  
  implicit val byUserWrites = Writes[(String, Long)] { case (username, value) => 
    Json.obj("username" -> username, "value" -> value) }
  
  implicit val byActionWrites = Writes[(ContributionAction.Value, Long)] { case (action, value) =>
    Json.obj("action" -> action, "value" -> value) }
 
  implicit val byItemTypeWrites = Writes[(ItemType.Value, Long)] { case (itemType, value) =>
    Json.obj("item_type" -> itemType, "value" -> value) }
  
  implicit val contributionHistoryWrites = Writes[(DateTime, Long)] { case (dateTime, value) =>
    Json.obj("date" -> dateTime, "value" -> value) }
  
  implicit val contributionStatsWrites: Writes[ContributionStats] = (
    (JsPath \ "took").write[Long] and
    (JsPath \ "total_contributions").write[Long] and
    (JsPath \ "by_user").write[Seq[(String, Long)]] and
    (JsPath \ "by_action").write[Seq[(ContributionAction.Value, Long)]] and
    (JsPath \ "by_item_type").write[Seq[(ItemType.Value, Long)]] and
    (JsPath \ "contribution_history").write[Seq[(DateTime, Long)]]
  )(unlift(ContributionStats.unapply))
  
}
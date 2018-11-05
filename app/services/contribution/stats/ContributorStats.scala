package services.contribution.stats

import org.joda.time.DateTime
import services.HasDate
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** Base stats for a specific contributor **/
case class ContributorStats(totalContributions: Long, overTime: Seq[(DateTime, Long)])

object ContributorStats extends HasDate {

  implicit val bucketTupleWrites = Writes[(DateTime, Long)] { case (datetime, value) =>
    Json.obj("date" -> datetime, "value" -> value) 
  }
  
  implicit val contributorStatsWrites: Writes[ContributorStats] = (
    (JsPath \ "total_contributions").write[Long] and
    (JsPath \ "over_time").write[Seq[(DateTime, Long)]]
  )(unlift(ContributorStats.unapply))

}
  

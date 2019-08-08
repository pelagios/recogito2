package services.contribution.feed

import com.sksamuel.elastic4s.searches.RichSearchResponse
import java.util.UUID
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.bucket.histogram.InternalDateHistogram
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.joda.time.{DateTime, DateTimeZone}
import scala.collection.JavaConverters._
import services.contribution.{ContributionAction, ItemType}

/** Document activity feed **/
case class DocumentActivityFeed(took: Long, activities: Seq[DayActivity])

/** Activity for one day **/
case class DayActivity(timestamp: DateTime, count: Long, users: Seq[ActivityByUser])

/** Nesting by document **/
case class ActivityByUser(username: String, count: Long, parts: Seq[ActivityByPart])

/** Nesting by document part **/
case class ActivityByPart(partId: UUID, count: Long, entries: Seq[ActivityFeedEntry])

/** Base unit of entry in the activity feed.
  * 
  * { action } { count } { item type }
  *
  * e.g. "Created  5 place bodies" or "Deleted 1 tag"
  */
case class ActivityFeedEntry(action: ContributionAction.Value, itemType: ItemType.Value, count: Long)

object DocumentActivityFeed {

  def fromSearchResponse(response: RichSearchResponse): DocumentActivityFeed = {
    val overTime = response.aggregations.getAs[InternalFilter]("over_time")
      .getAggregations.get("per_day").asInstanceOf[InternalDateHistogram]

    val activities = overTime.getBuckets.asScala.map { bucket => 
      val timestamp = new DateTime(bucket.getKey.asInstanceOf[DateTime].getMillis, DateTimeZone.UTC)

      val byUser: Seq[ActivityByUser] = bucket.getAggregations.get("by_user").asInstanceOf[Terms]
        .getBuckets.asScala.map { bucket =>           

          val byPart: Seq[ActivityByPart] = bucket.getAggregations.get("by_part").asInstanceOf[Terms]
            .getBuckets.asScala.map { bucket => 
              // ActivityEntry is a flattened version of the two last nesting levels (action and type)
              val entries = bucket.getAggregations.get("by_action").asInstanceOf[Terms]
                .getBuckets.asScala.flatMap { bucket => 
                  val thisAction = ContributionAction.withName(bucket.getKeyAsString)

                  bucket.getAggregations.get("by_item_type").asInstanceOf[Terms]
                    .getBuckets.asScala.map { bucket => 
                      ActivityFeedEntry(thisAction, ItemType.withName(bucket.getKeyAsString), bucket.getDocCount)
                    }              
                }
              ActivityByPart(UUID.fromString(bucket.getKeyAsString), bucket.getDocCount, entries)
            } 
          ActivityByUser(bucket.getKeyAsString, bucket.getDocCount, byPart)
        }
      DayActivity(timestamp, bucket.getDocCount, byUser)
    }
    DocumentActivityFeed(response.tookInMillis, activities)
  }

}
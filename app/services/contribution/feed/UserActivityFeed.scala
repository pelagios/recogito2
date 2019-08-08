package services.contribution.feed

import com.sksamuel.elastic4s.searches.RichSearchResponse
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.bucket.histogram.InternalDateHistogram
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.joda.time.{DateTime, DateTimeZone}
import scala.collection.JavaConverters._
import services.ContentType
import services.contribution.{ContributionAction, ItemType}

case class UserActivityFeed(took: Long, activities: Seq[UserActivityPerDay])

/** All users, one day */
case class UserActivityPerDay(timestamp: DateTime, count: Long, users: Seq[ActivityPerUser])

case class ActivityPerUser(username: String, count: Long, parts: Seq[UserActivityPerDocument])

case class UserActivityPerDocument(documentId: String, count: Long, entries: Seq[UserActivityFeedEntry])

/** Base unit of entry in the user activity feed.
  * 
  * { action } { count } { item type } { content type}
  *
  * e.g. "Created  5 place bodies on image" or "Deleted 1 tag on TEI"
  */
case class UserActivityFeedEntry(
  action: ContributionAction.Value, 
  itemType: ItemType.Value, 
  contentType: ContentType, 
  count: Long)

object UserActivityFeed {

  def fromSearchResponse(response: RichSearchResponse) = {
    val overTime = response.aggregations.getAs[InternalFilter]("over_time")
      .getAggregations.get("per_day").asInstanceOf[InternalDateHistogram]

    val activities = overTime.getBuckets.asScala.map { bucket => 
      val timestamp = new DateTime(bucket.getKey.asInstanceOf[DateTime].getMillis, DateTimeZone.UTC)

      val byUser: Seq[ActivityPerUser] = bucket.getAggregations.get("by_user").asInstanceOf[Terms]
        .getBuckets.asScala.map { bucket =>
          
          val byDocument: Seq[UserActivityPerDocument] = bucket.getAggregations.get("by_doc_id").asInstanceOf[Terms]
            .getBuckets.asScala.map { bucket => 
              // ActivityEntry is a flattened version of the three last nesting levels (action, item type and content type)  
              val entries = bucket.getAggregations.get("by_action").asInstanceOf[Terms]
                .getBuckets.asScala.flatMap { bucket => 
                  val thisAction = ContributionAction.withName(bucket.getKeyAsString)

                  bucket.getAggregations.get("by_item_type").asInstanceOf[Terms]
                    .getBuckets.asScala.flatMap { bucket => 
                      val thisType = ItemType.withName(bucket.getKeyAsString)

                      bucket.getAggregations.get("by_content_type").asInstanceOf[Terms]
                        .getBuckets.asScala.flatMap { bucket => 
                          // Content types also include "super types" ("TEXT", "IMAGE") which exist as 
                          // shorthands to simplify ES query, but are not actual valid content 
                          // types - just skip those
                          val maybeContentType = ContentType.withName(bucket.getKeyAsString)
                          maybeContentType.map { contentType => 
                            UserActivityFeedEntry(thisAction, thisType, contentType, bucket.getDocCount)
                          }
                        }
                    }
                }
              UserActivityPerDocument(bucket.getKeyAsString, bucket.getDocCount, entries)
            }
          ActivityPerUser(bucket.getKeyAsString, bucket.getDocCount, byDocument)
        }
      UserActivityPerDay(timestamp, bucket.getDocCount, byUser)
    }
    UserActivityFeed(response.tookInMillis, activities)
  }

}
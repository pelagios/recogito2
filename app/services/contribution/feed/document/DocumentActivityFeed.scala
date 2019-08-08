package services.contribution.feed.document

import com.sksamuel.elastic4s.searches.RichSearchResponse
import java.util.UUID
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.bucket.histogram.InternalDateHistogram
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.joda.time.{DateTime, DateTimeZone}
import scala.collection.JavaConverters._
import services.contribution.{ContributionAction, ItemType}

/** Activity feed for a specific document **/
case class DocumentActivityFeed(documentId: String, took: Long, activities: Seq[DocumentDayActivity])

object DocumentActivityFeed {

  def fromSearchResponse(documentId: String, response: RichSearchResponse) = {
    val overTime = response.aggregations.getAs[InternalFilter]("over_time")
      .getAggregations.get("per_day").asInstanceOf[InternalDateHistogram]

    val activities = overTime.getBuckets.asScala.map { bucket => 
      val timestamp = new DateTime(bucket.getKey.asInstanceOf[DateTime].getMillis, DateTimeZone.UTC)

      val byUser: Seq[DocumentActivityByUser] = bucket.getAggregations.get("by_user").asInstanceOf[Terms]
        .getBuckets.asScala.map { bucket =>           

          val byPart: Seq[DocumentActivityByPart] = bucket.getAggregations.get("by_part").asInstanceOf[Terms]
            .getBuckets.asScala.map { bucket => 
              // ActivityEntry is a flattened version of the two last nesting levels (action and type)
              val entries = bucket.getAggregations.get("by_action").asInstanceOf[Terms]
                .getBuckets.asScala.flatMap { bucket => 
                  val thisAction = ContributionAction.withName(bucket.getKeyAsString)

                  bucket.getAggregations.get("by_item_type").asInstanceOf[Terms]
                    .getBuckets.asScala.map { bucket => 
                      DocumentActivityFeedEntry(thisAction, ItemType.withName(bucket.getKeyAsString), bucket.getDocCount)
                    }              
                }
              DocumentActivityByPart(UUID.fromString(bucket.getKeyAsString), bucket.getDocCount, entries)
            } 
          DocumentActivityByUser(bucket.getKeyAsString, bucket.getDocCount, byPart)
        }
      DocumentDayActivity(timestamp, bucket.getDocCount, byUser)
    }
    DocumentActivityFeed(documentId, response.tookInMillis, activities)
  }

}
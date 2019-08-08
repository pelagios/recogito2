package services.contribution.feed.user

import com.sksamuel.elastic4s.searches.RichSearchResponse
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.bucket.histogram.InternalDateHistogram
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.joda.time.{DateTime, DateTimeZone}
import play.api.mvc.{AnyContent, Request}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import services.ContentType
import services.contribution.{ContributionAction, ItemType}
import services.document.DocumentService
import services.user.User

object UserActivityFeed {

  private def parseAggregations(response: RichSearchResponse) = {
    val overTime = response.aggregations.getAs[InternalFilter]("over_time")
      .getAggregations.get("per_day").asInstanceOf[InternalDateHistogram]

    // Note: ES result is in ascending order, but we want descending (most recent first)
    overTime.getBuckets.asScala.toSeq.reverse.map { bucket => 
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
              RawUserActivityPerDocument(bucket.getKeyAsString, bucket.getDocCount, entries)
            }
          ActivityPerUser(bucket.getKeyAsString, bucket.getDocCount, byDocument)
        }
      UserActivityPerDay(timestamp, bucket.getDocCount, byUser)
    }
  }

  def fromSearchResponse(
    loggedInAs: Option[String], response: RichSearchResponse
  )(implicit 
      documents: DocumentService, 
      request: Request[AnyContent],
      ctx: ExecutionContext
  ) = {
    val rawFeed = parseAggregations(response)
    
    // Get all distinct doc IDs in the feed and check if the current user has read permissions
    val docIds = rawFeed.flatMap { perDay => 
      perDay.users.flatMap { perUser => 
        perUser.documents.map { _.asInstanceOf[RawUserActivityPerDocument].documentId }
      }
    }.toSeq.distinct

    documents.getDocumentRecordsByIdWithAccessLevel(docIds, loggedInAs).map { docsAndPermissions => 
      val docs = docsAndPermissions.map(_._1)

      // TODO filter by permission
      
      rawFeed.map(_.enrich(docs))
    }
  }

}
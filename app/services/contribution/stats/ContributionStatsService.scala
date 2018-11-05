package services.contribution.stats

import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.aggregations.bucket.histogram.{DateHistogramInterval, InternalDateHistogram}
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.joda.time.{DateTime, DateTimeZone}
import scala.collection.JavaConverters._
import scala.concurrent.Future
import services.contribution._
import storage.es.ES

trait ContributionStatsService { self: ContributionService =>
  
  def getContributorStats(username: String) =
    es.client execute {
      search (ES.RECOGITO / ES.CONTRIBUTION) query {
        termQuery("made_by" -> username)
      } aggs (
        filterAggregation("over_time") query rangeQuery("made_at").gt("now-3M") subaggs (
          dateHistogramAggregation("last_3_months") field "made_at" minDocCount 0 interval DateHistogramInterval.WEEK
        )
      ) limit 0
    } map { response => 
      val totalEdits = response.totalHits
      val lastThreeMonths = response.aggregations.getAs[InternalFilter]("over_time")
        .getAggregations.get("last_3_months").asInstanceOf[InternalDateHistogram]

      ContributorStats(
        response.totalHits,
        lastThreeMonths.getBuckets.asScala.map(bucket =>
          (new DateTime(bucket.getKey.asInstanceOf[DateTime].getMillis, DateTimeZone.UTC), bucket.getDocCount)))
    }

  def getTopDocuments(username: String) = ???

  /** Returns the system-wide contribution stats **/
  def getSystemStats(): Future[SystemStats] =
    es.client execute {
      search (ES.RECOGITO / ES.CONTRIBUTION) aggs (
        termsAggregation("by_user") field "made_by",
        termsAggregation("by_action") field "action",
        termsAggregation("by_item_type") field "affects_item.item_type",
        filterAggregation("contribution_history") query rangeQuery("made_at").gt("now-30d") subaggs (
          dateHistogramAggregation("last_30_days") field "made_at" minDocCount 0 interval DateHistogramInterval.DAY
        )
      ) limit 0
    } map { response =>
      val byUser = response.aggregations.termsResult("by_user")
      val byAction = response.aggregations.termsResult("by_action")
      val byItemType = response.aggregations.termsResult("by_item_type")
      val contributionHistory = response.aggregations.getAs[InternalFilter]("contribution_history")
        .getAggregations.get("last_30_days").asInstanceOf[InternalDateHistogram]

      SystemStats(
        response.tookInMillis,
        response.totalHits,
        byUser.getBuckets.asScala.map(bucket =>
          (bucket.getKeyAsString, bucket.getDocCount)),
        byAction.getBuckets.asScala.map(bucket =>
          (ContributionAction.withName(bucket.getKeyAsString), bucket.getDocCount)),
        byItemType.getBuckets.asScala.map(bucket =>
          (ItemType.withName(bucket.getKeyAsString), bucket.getDocCount)),
        contributionHistory.getBuckets.asScala.map(bucket =>
          (new DateTime(bucket.getKey.asInstanceOf[DateTime].getMillis, DateTimeZone.UTC), bucket.getDocCount)))
    }

}
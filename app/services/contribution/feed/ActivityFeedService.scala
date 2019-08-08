package services.contribution.feed

import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval
import play.api.mvc.{AnyContent, Request}
import services.contribution._
import services.contribution.feed.user.UserActivityFeed
import services.contribution.feed.document.DocumentActivityFeed
import services.document.DocumentService
import services.user.User
import storage.es.ES

trait ActivityFeedService { self: ContributionService =>

  def getUserActivityFeed(
    usernames: Seq[String], loggedInAs: Option[String]
  )(implicit request: Request[AnyContent], documents: DocumentService) = 
    es.client execute {
      search (ES.RECOGITO / ES.CONTRIBUTION) query {
        boolQuery
          should {
            usernames.map(username => termQuery("made_by" -> username))
          }
      } aggs (
        filterAggregation("over_time") query rangeQuery("made_at").gt("now-3M") subaggs (
          dateHistogramAggregation("per_day") field "made_at" minDocCount 0 interval DateHistogramInterval.DAY subaggs (
            termsAggregation("by_user") field "made_by" subaggs (
              termsAggregation("by_doc_id") field "affects_item.document_id" subaggs (
                termsAggregation("by_action") field "action" subaggs (
                  termsAggregation("by_item_type") field "affects_item.item_type" subaggs (
                    termsAggregation("by_content_type") field "affects_item.content_type"
                  )
                )
              )
            )
          )
        )
      ) size 0
    } flatMap { response => UserActivityFeed.fromSearchResponse(loggedInAs, response) }

  def getDocumentActivityFeed(
    docId: String
  )(implicit request: Request[AnyContent], documents: DocumentService) = 
    es.client execute {
      search (ES.RECOGITO / ES.CONTRIBUTION) query {
        termQuery("affects_item.document_id", docId)
      } aggs (
        filterAggregation("over_time") query rangeQuery("made_at").gt("now-3M") subaggs (
          dateHistogramAggregation("per_day") field "made_at" minDocCount 0 interval DateHistogramInterval.DAY subaggs (
            termsAggregation("by_user") field "made_by" subaggs (
              termsAggregation("by_part") field "affects_item.filepart_id" subaggs (
                termsAggregation("by_action") field "action" subaggs (
                  termsAggregation("by_item_type") field "affects_item.item_type"
                )
              )
            )
          )
        )
      ) size 0
    } flatMap { response => DocumentActivityFeed.fromSearchResponse(docId, response) }

}
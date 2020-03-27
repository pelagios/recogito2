package services.annotation.stats

import collection.JavaConverters._
import com.sksamuel.elastic4s.ElasticDsl._
import javax.inject.{Inject, Singleton}
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.HasAnnotationIndexing
import storage.es.ES
import services.annotation.{AnnotationService, AnnotationBody, AnnotationStatus}
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms

case class StatusRatio(verified: Long, unverified: Long, notIdentifiable: Long)

@Singleton
trait AnnotationStatsService { self: AnnotationService =>
  
  def getTagStats(documentId: String) =
    es.client execute {
      search (ES.RECOGITO / ES.ANNOTATION) query {
        termQuery("annotates.document_id" -> documentId)
      } aggregations (
        nestedAggregation("per_body", "bodies") subaggs (
          filterAggregation("tags_only").query(termQuery("bodies.type" -> AnnotationBody.TAG.toString)) subaggs (
            termsAggregation("by_tag") field ("bodies.value.raw") size 500
          )
        )
      ) size 0 
    } map { _.aggregations
      .getAs[InternalNested]("per_body").getAggregations
      .get[InternalFilter]("tags_only").getAggregations
      .get[StringTerms]("by_tag").getBuckets.asScala
      .map(b => (b.getKeyAsString, b.getDocCount))
    }

  def getStatusRatios(documentIds: Seq[String]): Future[Map[String, StatusRatio]] =
    es.client execute {
      search (ES.RECOGITO / ES.ANNOTATION) query {
        boolQuery
          should {
            documentIds.map(id => termQuery("annotates.document_id" -> id))
          }
      } aggregations (
        termsAggregation("per_document") field ("annotates.document_id") subaggs (
          nestedAggregation("per_body", "bodies") subaggs (
            termsAggregation("by_status_value") field ("bodies.status.value")
          )
        )
      ) size 0
    } map { _.aggregations
      .termsResult("per_document")
      .getBuckets.asScala.map { docBucket => 
        val docId = docBucket.getKeyAsString
        val statusValues = docBucket
          .getAggregations.get[InternalNested]("per_body")
          .getAggregations.get[StringTerms]("by_status_value")
          .getBuckets.asScala.map { statusBucket =>
            val status = AnnotationStatus.withName(statusBucket.getKeyAsString)
            val count = statusBucket.getDocCount
            (status, count)
          }.toMap
        
        val ratio = StatusRatio(
          statusValues.get(AnnotationStatus.VERIFIED).getOrElse(0l),
          statusValues.get(AnnotationStatus.UNVERIFIED).getOrElse(0l),
          statusValues.get(AnnotationStatus.NOT_IDENTIFIABLE).getOrElse(0l)
        )

        (docId, ratio)
      }.toMap
    }
  
}
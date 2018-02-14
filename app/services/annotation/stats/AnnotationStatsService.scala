package services.annotation.stats

import collection.JavaConverters._
import com.sksamuel.elastic4s.ElasticDsl._
import javax.inject.{Inject, Singleton}
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested
import scala.concurrent.ExecutionContext
import services.annotation.HasAnnotationIndexing
import storage.es.ES
import services.annotation.AnnotationBody
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms

@Singleton
class AnnotationStatsService @Inject() (
  implicit val ctx: ExecutionContext,
  implicit val es: ES
) extends HasAnnotationIndexing {
  
  def getTagStats(documentId: String) =
    es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION) query {
        termQuery("annotates.document_id" -> documentId)
      } aggregations (
        nestedAggregation("per_body", "bodies") subaggs (
          filterAggregation("tags_only").query(termQuery("bodies.type" -> AnnotationBody.TAG.toString)) subaggs (
            termsAggregation("by_tag") field ("bodies.value.raw")
          )
        )
      ) size 0 
    } map { _.aggregations
      .getAs[InternalNested]("per_body").getAggregations
      .get[InternalFilter]("tags_only").getAggregations
      .get[StringTerms]("by_tag").getBuckets.asScala
      .map(b => (b.getKeyAsString, b.getDocCount))
    }
  
}
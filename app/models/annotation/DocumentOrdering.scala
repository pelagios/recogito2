package models.annotation

import com.sksamuel.elastic4s.ElasticDsl._
import storage.ES
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import scala.collection.JavaConverters._
import scala.language.implicitConversions

trait DocumentOrdering { self: AnnotationService =>
  
  implicit def convertSortOrder(order: models.SortOrder): SortOrder =
    order match {
      case models.SortOrder.DESC => SortOrder.DESC
      case _ => SortOrder.ASC
    }
  
  private def getOrderByField(docIds: Seq[String], fieldname: String, sortOrder: models.SortOrder, limit: Int) = 
    self.es.client execute {
      search in ES.RECOGITO / ES.ANNOTATION query {
        nestedQuery("annotates").query {
          bool {
            should {
              docIds.map(id => termQuery("annotates.document_id" -> id))
            }
          }
        }
      } limit limit sort ( field sort fieldname order sortOrder)
    }
    
  def getOrderByLastModifiedBy(docIds: Seq[String], sortOrder: models.SortOrder, limit: Int) =
    getOrderByField(docIds, "last_modified_by", sortOrder, limit)
  
  def getOrderByLastModifiedAt(docIds: Seq[String], sortOrder: models.SortOrder, limit: Int) =
    getOrderByField(docIds, "last_modified_at", sortOrder, limit)

  def getOrderByAnnotationCount(docIds: Seq[String], sortOrder: SortOrder) = 
    self.es.client execute {
      search in ES.RECOGITO / ES.ANNOTATION query {
        nestedQuery("annotates").query {
          bool {
            should {
              docIds.map(id => termQuery("annotates.document_id" -> id))
            }
          }
        }
      } aggs {
        aggregation nested("by_document") path "annotates" aggs (
          aggregation terms "document_id" field "annotates.document_id"
        )
      } limit 0
    } map { response =>
      val byDocument = response.getAggregations.get("by_document").asInstanceOf[Nested]
        .getAggregations.get("document_id").asInstanceOf[Terms]
      
      byDocument.getBuckets.asScala.map(bucket => (bucket.getKey, bucket.getDocCount)).toSeq
    }

}
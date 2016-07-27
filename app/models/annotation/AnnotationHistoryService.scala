package models.annotation

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import java.util.UUID
import models.HasDate
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import storage.ES

trait AnnotationHistoryService extends HasAnnotationIndexing with HasDate {

  private val ANNOTATION_HISTORY = "annotation_history"
  
  implicit object AnnotationHistoryRecordIndexable extends Indexable[AnnotationHistoryRecord] {
    override def json(a: AnnotationHistoryRecord): String = Json.stringify(Json.toJson(a))
  }

  implicit object AnnotationHistoryRecordHitAs extends HitAs[AnnotationHistoryRecord] {
    override def as(hit: RichSearchHit): AnnotationHistoryRecord =
      Json.fromJson[AnnotationHistoryRecord](Json.parse(hit.sourceAsString)).get
  }

  /** Inserts a new version into the history index **/
  def insertVersion(annotation: Annotation)(implicit es: ES, context: ExecutionContext): Future[Boolean] =
    es.client execute {
      index into ES.IDX_RECOGITO / ANNOTATION_HISTORY source AnnotationHistoryRecord.forVersion(annotation)
    } map {
      _.isCreated
    } recover { case t: Throwable =>
      Logger.error("Error storing annotation version")
      Logger.error(t.toString)
      t.printStackTrace
      false
    }
  
  /** Inserts a delete marker for the (now deleted) annotation **/
  def insertDeleteMarker(annotation: Annotation, deletedBy: String, deletedAt: DateTime)(implicit es: ES, context: ExecutionContext): Future[Boolean] =
    es.client execute {
      index into ES.IDX_RECOGITO / ANNOTATION_HISTORY source AnnotationHistoryRecord.forDelete(annotation, deletedBy, deletedAt)
    } map {
      _.isCreated
    } recover { case t: Throwable =>
      Logger.error("Error storing delete marker")
      Logger.error(t.toString)
      t.printStackTrace
      false    
    }
    
  /** Returns the IDs of annotations that were changed after the given timestamp **/
  def getChangedAfter(docId: String, after: DateTime)(implicit es: ES, context: ExecutionContext): Future[Seq[String]] =
    es.client execute {
      search in ES.IDX_RECOGITO / ANNOTATION_HISTORY query {
        filteredQuery query {
          nestedQuery("annotates").query(termQuery("annotates.document_id" -> docId))
        } filter {
          rangeFilter("last_modified_at").gt(formatDate(after))
        }
      } aggs {
        aggregation terms "by_annotation_id" field "annotation_id" size Int.MaxValue
      }
    } map { response => 
      response
        .getAggregations.get("by_annotation_id").asInstanceOf[Terms]
        .getBuckets.asScala.map(_.getKey)
    }
    
  def getAnnotationStateAt(annotationId: String, time: DateTime)(implicit es: ES, context: ExecutionContext): Future[Option[AnnotationHistoryRecord]] =
    es.client execute {
      search in ES.IDX_RECOGITO / ANNOTATION_HISTORY query {
        filteredQuery query {
          termQuery("annotation_id" -> annotationId)
        } filter {
          rangeFilter("last_modified_at").lte(formatDate(time))
        }
      } sort {
        field sort "last_modified_at" order SortOrder.DESC
      } limit 1
    } map { _.as[AnnotationHistoryRecord].toSeq.headOption }

  /** Deletes all history records (versions and delete markers) for a document, after a given timestamp **/
  def deleteHistoryRecordsAfter(docId: String, after: DateTime)(implicit es: ES, context: ExecutionContext): Future[Boolean] = {

    // Retrieves all versions on the document after the the given timestamp
    def findHistoryRecordsAfter() = es.client execute {
      search in ES.IDX_RECOGITO / ANNOTATION_HISTORY query filteredQuery query {
        nestedQuery("annotates").query(termQuery("annotates.document_id" -> docId))
      } postFilter { // TODO remove postFilter!
        rangeFilter("last_modified_at").gt(formatDate(after))
      } limit Int.MaxValue
    } map { _.getHits.getHits }

    findHistoryRecordsAfter().flatMap { hits =>
      if (hits.size > 0) {
        es.client execute {
          bulk ( hits.map(h => delete id h.getId from ES.IDX_RECOGITO / ANNOTATION_HISTORY) )
        } map {
          !_.hasFailures
        } recover { case t: Throwable =>
          t.printStackTrace()
          false
        }
      } else {
        // Nothing to delete
        Future.successful(true)
      }
    }
  }

}

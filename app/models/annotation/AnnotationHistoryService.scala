package models.annotation

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import com.sksamuel.elastic4s.ElasticDsl._
import java.util.UUID
import models.{HasDate, HasTryToEither}
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.reflectiveCalls
import scala.util.Try
import storage.ES

trait AnnotationHistoryService extends HasAnnotationIndexing with HasDate { self: AnnotationHistoryService =>

  implicit object AnnotationHistoryRecordIndexable extends Indexable[AnnotationHistoryRecord] {
    override def json(a: AnnotationHistoryRecord): String = Json.stringify(Json.toJson(a))
  }

  implicit object AnnotationHistoryRecordHitReader extends HitReader[AnnotationHistoryRecord] with HasTryToEither {
    override def read(hit: Hit): Either[Throwable, AnnotationHistoryRecord] =
      Try(Json.fromJson[AnnotationHistoryRecord](Json.parse(hit.sourceAsString)).get)
  }

  /** Inserts a new version into the history index **/
  def insertVersion(annotation: Annotation)(implicit es: ES, context: ExecutionContext): Future[Boolean] =
    es.client execute {
      indexInto(ES.RECOGITO / ES.ANNOTATION_HISTORY) source AnnotationHistoryRecord.forVersion(annotation)
    } map {
      _.created
    } recover { case t: Throwable =>
      Logger.error("Error storing annotation version")
      Logger.error(t.toString)
      t.printStackTrace
      false
    }

  /** Inserts a delete marker for the (now deleted) annotation **/
  def insertDeleteMarker(annotation: Annotation, deletedBy: String, deletedAt: DateTime)(implicit es: ES, context: ExecutionContext): Future[Boolean] =
    es.client execute {
      indexInto(ES.RECOGITO / ES.ANNOTATION_HISTORY) source AnnotationHistoryRecord.forDelete(annotation, deletedBy, deletedAt)
    } map {
      _.created
    } recover { case t: Throwable =>
      Logger.error("Error storing delete marker")
      Logger.error(t.toString)
      t.printStackTrace
      false
    }

  /** Returns the IDs of annotations that were changed after the given timestamp **/
  def getChangedAfter(docId: String, after: DateTime)(implicit es: ES, context: ExecutionContext): Future[Seq[String]] =
    es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION_HISTORY) query {
        boolQuery
          must (
            termQuery("annotates.document_id" -> docId)
          ) filter (
            rangeQuery("last_modified_at").gt(formatDate(after))
          )
      } aggs {
        termsAggregation("by_annotation_id") field "annotation_id" size ES.MAX_SIZE
      }
    } map { response =>
      response.aggregations.termsResult("by_annotation_id")
        .getBuckets.asScala.map(_.getKeyAsString)
    }

  def getAnnotationStateAt(annotationId: String, time: DateTime)(implicit es: ES, context: ExecutionContext): Future[Option[AnnotationHistoryRecord]] =
    es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION_HISTORY) query {
        boolQuery
          must (
            termQuery("annotation_id" -> annotationId)
          ) filter (
            rangeQuery("last_modified_at").lte(formatDate(time))
          )
      } sortBy {
        fieldSort("last_modified_at") order SortOrder.DESC
      } limit 1
    } map { _.to[AnnotationHistoryRecord].toSeq.headOption }

  /** Deletes all history records (versions and delete markers) for a document, after a given timestamp **/
  def deleteHistoryRecordsAfter(docId: String, after: DateTime)(implicit es: ES, context: ExecutionContext): Future[Boolean] = {

    // Retrieves all versions on the document after the the given timestamp
    def findHistoryRecordsAfter() = es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION_HISTORY) query {
        boolQuery
          must (
            termQuery("annotates.document_id" -> docId)
          ) filter (
            rangeQuery("last_modified_at").gt(formatDate(after))
          )
      } limit ES.MAX_SIZE
    } map { _.hits }

    findHistoryRecordsAfter().flatMap { hits =>
      if (hits.size > 0) {
        es.client.java.prepareBulk()
        es.client execute {
          bulk ( hits.map(h => delete(h.id) from ES.RECOGITO / ES.ANNOTATION_HISTORY))
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

  /** Deletes all history records (versions and delete markers) for a document **/
  def deleteHistoryRecordsByDocId(docId: String)(implicit es: ES, context: ExecutionContext): Future[Boolean] = {
    def findRecords() = es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION_HISTORY) query {
        termQuery("annotates.document_id" -> docId)
      } limit ES.MAX_SIZE
    } map { _.hits }

    findRecords.flatMap { hits =>
      if (hits.size > 0) {
        es.client.java.prepareBulk()
        es.client execute {
          bulk (hits.map(h => delete(h.id) from ES.RECOGITO / ES.ANNOTATION_HISTORY))
        } map { response =>
          if (response.hasFailures)
            Logger.error("Failures while deleting annotation versions: " + response.failureMessage)
          !response.hasFailures
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

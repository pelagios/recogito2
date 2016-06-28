package models.annotation

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import java.util.UUID
import models.geotag.ESGeoTagStore
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import storage.ES

/** Encapsulates JSON (de-)serialization so we can add it to Annotation- and AnnotationHistoryService **/
trait HasAnnotationIndexing {

  implicit object AnnotationIndexable extends Indexable[Annotation] {
    override def json(a: Annotation): String = Json.stringify(Json.toJson(a))
  }

  implicit object AnnotationHitAs extends HitAs[(Annotation, Long)] {
    override def as(hit: RichSearchHit): (Annotation, Long) =
      (Json.fromJson[Annotation](Json.parse(hit.sourceAsString)).get, hit.version)
  }

}

object AnnotationService extends HasAnnotationIndexing with AnnotationHistoryService with ESGeoTagStore {

  private val ANNOTATION = "annotation"

  private def MAX_RETRIES = 10 // Max number of import retires in case of failure

  /** Upserts an annotation, automatically dealing with updating version history and geotags.
    *
    * Returns a boolean flag indicating successful completion, the internal ElasticSearch
    * version, and the previous version of the annotation, if any.
    */
  def insertOrUpdateAnnotation(annotation: Annotation, versioned: Boolean = false)(implicit context: ExecutionContext): Future[(Boolean, Long, Option[Annotation])] = {

    // Persists the previous version of an annotation to the history index; returns a
    // boolean flag indicating successful completion, plus the previous annotation version if any
    def persistPreviousVersion(annotationId: UUID): Future[(Boolean, Option[Annotation])] =
      for {
        maybeAnnotation <- findById(annotationId)
        done <- if (maybeAnnotation.isDefined) insertVersion(maybeAnnotation.get._1) else Future.successful(true)
      } yield (done, maybeAnnotation.map(_._1))

    // Upsert annotation in the annotation index; returns a boolean flag indicating success, plus
    // the internal ElasticSearch version number
    def upsertAnnotation(a: Annotation): Future[(Boolean, Long)] =
      ES.client execute {
        update id a.annotationId in ES.IDX_RECOGITO / ANNOTATION source a docAsUpsert
      } map { r =>
        (true, r.getVersion)
      } recover { case t: Throwable =>
        Logger.error("Error indexing annotation " + annotation.annotationId + ": " + t.getMessage)
        t.printStackTrace
        (false, -1l)
      }

    for {
      (versioningDone, previousAnnotation) <- if (versioned) persistPreviousVersion(annotation.annotationId) else Future.successful((true, None))
      (annotationCreated, esVersionNumber) <- upsertAnnotation(annotation)
      linksCreated <- if (annotationCreated) insertOrUpdateGeoTagsForAnnotation(annotation) else Future.successful(false)
    } yield (linksCreated, esVersionNumber, previousAnnotation)

  }

  /** Upserts a list of annotations, handling non-blocking chaining & retries in case of failure **/
  def insertOrUpdateAnnotations(annotations: Seq[Annotation], retries: Int = MAX_RETRIES)(implicit context: ExecutionContext): Future[Seq[Annotation]] =
    annotations.foldLeft(Future.successful(Seq.empty[Annotation])) { case (future, annotation) =>
      future.flatMap { failedAnnotations =>
        insertOrUpdateAnnotation(annotation).map { case (success, _, _) =>
          if (success) failedAnnotations else failedAnnotations :+ annotation
        }
      }
    } flatMap { failed =>
      if (failed.size > 0 && retries > 0) {
        Logger.warn(failed.size + " annotations failed to import - retrying")
        insertOrUpdateAnnotations(failed, retries - 1)
      } else {
        Logger.info("Successfully imported " + (annotations.size - failed.size) + " annotations")
        if (failed.size > 0)
          Logger.error(failed.size + " annotations failed without recovery")
        else
          Logger.info("No failed imports")
        Future.successful(failed)
      }
    }

  /** Retrieves an annotation by ID **/
  def findById(annotationId: UUID)(implicit context: ExecutionContext): Future[Option[(Annotation, Long)]] =
    ES.client execute {
      get id annotationId.toString from ES.IDX_RECOGITO / ANNOTATION
    } map { response =>
      if (response.isExists) {
        val source = Json.parse(response.getSourceAsString)
        Some((Json.fromJson[Annotation](source).get, response.getVersion))
      } else {
        None
      }
    }

  /** Deletes the annotation with the given ID **/
  def deleteAnnotation(annotationId: UUID)(implicit context: ExecutionContext): Future[Boolean] =
    ES.client execute {
      delete id annotationId.toString from ES.IDX_RECOGITO / ANNOTATION
    } flatMap { response =>
      if (response.isFound)
        deleteGeoTagsByAnnotation(annotationId)
      else
        Future.successful(false)
    } recover { case t: Throwable =>
      t.printStackTrace()
      false
    }
    
  def countByDocId(id: String)(implicit context: ExecutionContext): Future[Long] =
    ES.client execute {
      count from ES.IDX_RECOGITO / ANNOTATION query nestedQuery("annotates").query(termQuery("annotates.document_id" -> id))
    } map { _.getCount }

  /** Retrieves all annotations on a given document **/
  def findByDocId(id: String, limit: Int = Int.MaxValue)(implicit context: ExecutionContext): Future[Seq[(Annotation, Long)]] =
    ES.client execute {
      search in ES.IDX_RECOGITO / ANNOTATION query nestedQuery("annotates").query(termQuery("annotates.document_id" -> id)) limit limit
    } map(_.as[(Annotation, Long)].toSeq)

  /** Deletes all annotations on a given document **/
  def deleteByDocId(docId: String)(implicit context: ExecutionContext): Future[Boolean] =
    findByDocId(docId).flatMap { annotationsAndVersions =>
      if (annotationsAndVersions.size > 0) {
        ES.client execute {
          bulk ( annotationsAndVersions.map { case (annotation, _) => delete id annotation.annotationId from ES.IDX_RECOGITO / ANNOTATION } )
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

  /** Retrieves all annotations on a given filepart **/
  def findByFilepartId(id: Int, limit: Int = Int.MaxValue)(implicit context: ExecutionContext): Future[Seq[(Annotation, Long)]] =
    ES.client execute {
      search in ES.IDX_RECOGITO / ANNOTATION query nestedQuery("annotates").query(termQuery("annotates.filepart_id" -> id)) limit limit
    } map(_.as[(Annotation, Long)].toSeq)

  /** Retrieves annotations on a document last updated after a given timestamp **/
  def findModifiedAfter(documentId: String, after: DateTime)(implicit context: ExecutionContext): Future[Seq[Annotation]] =
    ES.client execute {
      search in ES.IDX_RECOGITO / ANNOTATION query filteredQuery query {
        nestedQuery("annotates").query(termQuery("annotates.document_id" -> documentId))
      } postFilter {
        rangeFilter("last_modified_at").gt(formatDate(after))
      } limit Int.MaxValue
    } map { _.as[(Annotation, Long)].toSeq.map(_._1) }

  /** Rolls back the document to the state at the given timestamp **/ 
  def rollbackToTimestamp(documentId: String, timestamp: DateTime)(implicit context: ExecutionContext): Future[Boolean] = {

    // Rolls back one annotation, i.e. updates to the latest version in the history or deletes
    def rollbackOne(annotation: Annotation): Future[Boolean] =
      findLatestVersion(annotation.annotationId).flatMap(_ match {
        case Some(version) =>
          insertOrUpdateAnnotation(version, false).map { case (success, _, _) => success }

        case None =>
          deleteAnnotation(annotation.annotationId)
      }).recover { case t: Throwable =>
        t.printStackTrace()
        Logger.warn("Rollback failed for " + annotation.annotationId)
        false
      }

    // Rolls back a list of annotations, i.e. updates to latest version in history or deletes
    def rollbackAnnotations(annotations: Seq[Annotation]): Future[Seq[Annotation]] =
      annotations.foldLeft(Future.successful(Seq.empty[Annotation])) { case (future, annotation) =>
        future.flatMap { failedAnnotations =>
          rollbackOne(annotation).map { success =>
            if (success) failedAnnotations else failedAnnotations :+ annotation
          }
        }
      }

    val numberOfFailedRollbacks = for {
      deleteVersionsSuccess <- deleteVersionsAfter(documentId, timestamp)
      annotationsToModify <- if (deleteVersionsSuccess) findModifiedAfter(documentId, timestamp) else Future.successful(Seq.empty[Annotation])
      failedUpdates <- rollbackAnnotations(annotationsToModify)
    } yield failedUpdates.size

    numberOfFailedRollbacks.map { failed =>
      Logger.warn(failed + " failed rollbacks")
      failed == 0
    }
  }

}

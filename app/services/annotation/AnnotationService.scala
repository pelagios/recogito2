package services.annotation

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.SearchDefinition
import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.relation.RelationService
import services.entity.builtin.{EntityService, IndexedEntity}
import storage.es.{ES, HasScrollProcessing}

@Singleton
class AnnotationService @Inject() (
  entityService: EntityService,
  implicit val ctx: ExecutionContext,
  implicit val es: ES
) extends HasAnnotationIndexing
    with AnnotationHistoryService
    with RelationService 
    with HasScrollProcessing {

  /** Upserts an annotation.
    *
    * Automatically deals with version history.
    *
    * @return a boolean flag indicating successful completion, the internal ElasticSearch
    * version, and the previous version of the annotation, if any.
    */
  def upsertAnnotation(annotation: Annotation, versioned: Boolean = true): Future[(Boolean, Option[Annotation])] = {
    val fResolveEntityReferences = {
      val entityURIs = annotation.bodies.flatMap(_.uri)
      val fResolved = Future.sequence(entityURIs.map(entityService.findByURI(_)))
      for {
        resolved <- fResolved
      } yield resolved.flatten
    }

    def upsertAnnotation(a: Annotation): Future[Boolean] =
      es.client execute {
        indexInto(ES.RECOGITO / ES.ANNOTATION).doc(a).id(a.annotationId.toString)
      } map { _ => true
      } recover { case t: Throwable =>
        Logger.error(s"Error indexing annotation ${annotation.annotationId}: ${t.getMessage}")
        t.printStackTrace
        false
      }

    for {
      resolvedEntities <- fResolveEntityReferences
      maybePrevious <- if (versioned) findById(annotation.annotationId)
                       else Future.successful(None)
      stored <- upsertAnnotation(addUnionIds(annotation, resolvedEntities))
      success <- if (stored) {
                   if (versioned) insertVersion(annotation)
                   else Future.successful(true)
                 } else {
                   Future.successful(false)
                 }
    } yield (success, maybePrevious.map(_._1))
  }

  def upsertAnnotations(annotations: Seq[Annotation], versioned: Boolean = true, retries: Int = ES.MAX_RETRIES): Future[Seq[Annotation]] =
    annotations.foldLeft(Future.successful(Seq.empty[Annotation])) { case (future, annotation) =>
      future.flatMap { failedAnnotations =>
        upsertAnnotation(annotation).map { case (success, _) =>
          if (success) failedAnnotations else failedAnnotations :+ annotation
        }
      }
    } flatMap { failed =>
      if (failed.size > 0 && retries > 0) {
        Logger.warn(failed.size + " annotations failed to import - retrying")
        upsertAnnotations(failed, versioned, retries - 1)
      } else {
        // Logger.info("Successfully upserted " + (annotations.size - failed.size) + " annotations")
        if (failed.size > 0)
          Logger.error(failed.size + " annotations failed without recovery")
        // else
        //  Logger.info("No failed imports")
        Future.successful(failed)
      }
    }
    
  def findById(annotationId: UUID): Future[Option[(Annotation, Long)]] =
    es.client execute {
      get(annotationId.toString) from ES.RECOGITO / ES.ANNOTATION
    } map { response =>
      if (response.exists)
        Some(response.to[(Annotation, Long)])
      else
        None
    }

  private def deleteById(annotationId: String): Future[Boolean] =
    es.client execute {
      delete(annotationId.toString) from ES.RECOGITO / ES.ANNOTATION
    } map { _ =>
      true
    } recover { case t: Throwable =>
      t.printStackTrace()
      false
    }

  /** Deletes the annotation with the given ID **/
  def deleteAnnotation(annotationId: UUID, deletedBy: String, deletedAt: DateTime): Future[Option[Annotation]] = {
    findById(annotationId).flatMap(_ match {
      case Some((annotation, _)) =>
        val f = for {
          relationsUpdated <- removeRelationsTo(annotation.annotationId)
          markerInserted <- insertDeleteMarker(annotation, deletedBy, deletedAt)
          if (markerInserted)
            deleted <- deleteById(annotationId.toString)
        } yield relationsUpdated && deleted

        f.map { success =>
          if (!success) throw new Exception("Error deleting annotation")
          Some(annotation)
        } recover { case t: Throwable =>
          t.printStackTrace()
          None
        }

      case None => Future.successful(None)
    })
  }

  def countTotal(): Future[Long] =
    es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION) limit 0
    } map { _.totalHits }

  def countByDocId(id: String): Future[Long] =
    es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION) query {
        termQuery("annotates.document_id" -> id)
      } limit 0
    } map { _.totalHits }
    
  private def scrollIfNeeded(query: SearchDefinition, offset: Int = 0, limit: Int = ES.MAX_SIZE): Future[Seq[(Annotation, Long)]] ={
    // Backend serialization format
    import services.annotation.BackendAnnotation._
          
    es.client execute { 
      query start offset limit limit
    } flatMap { response =>
      val total = response.totalHits
      if (total > ES.MAX_SIZE) {
        // Oversized doc... use scroll API
        scrollQuery(query).map(_.map { hit =>
          (Json.fromJson[Annotation](Json.parse(hit.sourceAsString)).get, hit.version)
        })
      } else {
        Future.successful(response.to[(Annotation, Long)].toSeq)
      }
    } 
  }
    
  /** All annotations on the given document **/
  def findByDocId(id: String, offset: Int = 0, limit: Int = ES.MAX_SIZE): Future[Seq[(Annotation, Long)]] = 
    scrollIfNeeded {
      search(ES.RECOGITO / ES.ANNOTATION) query {
        termQuery("annotates.document_id" -> id)
      }
    }
  
  /** Retrieves all annotations on a given filepart **/
  def findByFilepartId(id: UUID, limit: Int = ES.MAX_SIZE): Future[Seq[(Annotation, Long)]] = 
    scrollIfNeeded {
      search(ES.RECOGITO / ES.ANNOTATION) query {
        termQuery("annotates.filepart_id" -> id.toString)
      }
    }

  /** Deletes all annotations & version history on a given document **/
  def deleteByDocId(docId: String): Future[Boolean] = {
    val deleteAnnotations = findByDocId(docId).flatMap { annotationsAndVersions =>
      if (annotationsAndVersions.size > 0) {
        es.client.java.prepareBulk()
        es.client execute {
          bulk ( annotationsAndVersions.map { case (annotation, _) => delete(annotation.annotationId.toString) from ES.RECOGITO / ES.ANNOTATION } )
        } map { response =>
          if (response.hasFailures)
            Logger.error("Failures while deleting annotations: " + response.failureMessage)
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

    val deleteVersions = deleteHistoryRecordsByDocId(docId)

    for {
      s1 <- deleteAnnotations
      s2 <- deleteVersions
    } yield (s1 && s2)
  }

  /** Retrieves annotations on a document last updated after a given timestamp **/
  def findModifiedAfter(documentId: String, after: DateTime): Future[Seq[Annotation]] =
    es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION) query {
        boolQuery
          must (
            termQuery("annotates.document_id" -> documentId)
          ) filter (
            rangeQuery("last_modified_at").gt(formatDate(after))
          )
      } limit ES.MAX_SIZE
    } map { _.to[(Annotation, Long)].toSeq.map(_._1) }

  /** Retrieves annotations carrying relations to the given ID and removes those relations **/
  def removeRelationsTo(relatedTo: UUID): Future[Boolean] = {
    // Fetches all annotations carrying relations to the given ID
    val fAffectedAnnotations = es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION) query {
        termQuery("relations.relates_to" -> relatedTo.toString)
      } limit ES.MAX_SIZE // Should be safe to assume no more than 10k annotations point here
    } map { _.to[(Annotation, Long)] }

    // Filters out relations that point to the given ID
    def removeRelations(annotations: Seq[Annotation]): Seq[Annotation] =
      annotations.map(removeRelationsTo(_, relatedTo))

    for {
      affected <- fAffectedAnnotations
      failed <- if (affected.isEmpty) Future.successful(Seq.empty[Annotation]) 
                else upsertAnnotations(removeRelations(affected.map(_._1)))
    } yield (failed.size == 0)
  }

  /** Rolls back the document to the state at the given timestamp **/
  def rollbackToTimestamp(documentId: String, timestamp: DateTime): Future[Boolean] = {

    // Rolls back one annotation, i.e. updates to the latest state recorded in the history or deletes
    def rollbackOne(annotationId: String): Future[Boolean] = {
      getAnnotationStateAt(annotationId, timestamp).flatMap(_ match {
        case Some(historyRecord) =>
          if (historyRecord.deleted)
            // The annotation was already deleted at the rollback state - do nothing
            Future.successful(true)
          else
            upsertAnnotation(historyRecord.asAnnotation, false).map(_._1)

        case None =>
          // The annotation did not exist at the rollback time - delete
          deleteById(annotationId)
      }).recover { case t: Throwable =>
        t.printStackTrace()
        Logger.warn("Rollback failed for " + annotationId)
        false
      }
    }

    // Rolls back a list of annotations, i.e. updates to latest state recorded in the history or deletes
    def rollbackAnnotations(annotations: Seq[String]): Future[Seq[String]] =
      annotations.foldLeft(Future.successful(Seq.empty[String])) { case (future, annotationId) =>
        future.flatMap { failedAnnotationIds =>
          rollbackOne(annotationId).map { success =>
            if (success) failedAnnotationIds else failedAnnotationIds :+ annotationId
          }
        }
      }

    val failedRollbacks = getChangedAfter(documentId, timestamp).flatMap(rollbackAnnotations)
    failedRollbacks.flatMap { failed =>
      if (failed.size == 0) {
        deleteHistoryRecordsAfter(documentId, timestamp)
      } else {
        Logger.warn(failed.size + " failed rollbacks")

        // TODO what would be a good recovery strategy?

        Future.successful(false)
      }
    }
  }

  /** Sorts the given list of document IDs by the number of annotations on the documents **/
  def sortDocsByAnnotationCount(docIds: Seq[String], sortOrder: services.SortOrder, offset: Int, limit: Int) = {

    import scala.collection.JavaConverters._

    val numberOfBuckets =
      if (sortOrder == services.SortOrder.ASC)
        offset + limit
      else
        docIds.size

    es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION) query {
        boolQuery
          should {
            docIds.map(id => termQuery("annotates.document_id" -> id))
          }
      } aggs {
        termsAggregation("by_document") field "annotates.document_id" size numberOfBuckets
      } limit 0
    } map { response =>
      val byDocument = response.aggregations.termsResult("by_document")
      val annotatedDocs = byDocument.getBuckets.asScala.map(_.getKeyAsString).toSeq
      val unannotatedDocs = (docIds diff annotatedDocs)
      val docs =
        if (sortOrder == services.SortOrder.ASC)
          (annotatedDocs ++ unannotatedDocs)
        else
          (annotatedDocs ++ unannotatedDocs).reverse

      docs.drop(offset).take(limit)
    }
  }

}

package services.entity.builtin

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.RichSearchResponse
import java.util.UUID
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.{Annotation, HasAnnotationIndexing, Reference}
import storage.es.{ES, HasScrollProcessing}

/** Implements the EntityServiceImpl's delete functionality.
  *
  * Due to the way our union index works, deleting entity records is a bit more complicated.
  * Union Entities must be retrieved from the index to remove individual EntityRecords and 
  * update or delete the Entity from the index; annotations that reference the record need to
  * be rewritten.
  * 
  * Since this requires a bit more code, we've split this into a separate source file.
  */
trait EntityDeleteImpl extends HasScrollProcessing with HasAnnotationIndexing { self: EntityServiceImpl =>
  
  private val RETRIES = 10
  
  /** The main delete loop. Runs a scroll query into ES to fetch all affected entities and then 
    * hands them to, batch by batch, to processEntityBatch for processing.   
    */
  override def deleteBySourceAuthority(authority: String): Future[Boolean] =
    es.client execute {
      search(ES.RECOGITO / ES.ENTITY) query {
        termQuery("is_conflation_of.source_authority" -> authority)
      } limit 50 version true scroll "5m"
    } flatMap { response => scroll(processEntityBatch(authority), response) } map { success =>
      if (success) Logger.info(s"Successfully removed records from ${authority}")
      else play.api.Logger.info(s"Delete process stopped. Something went wrong while removing records from ${authority}")
      success
    }
    
  /** The 'orchestration logic' for processesing one batch of entities **/
  private def processEntityBatch(authority: String)(response: RichSearchResponse): Future[Boolean] = {
    // Each entity will contain at least one record from the given authority. First, we'll remove those.
    // If there are any records (from different authorities) left, we return the modified record. If not,
    // we return None.
    def processOneEntity(e: IndexedEntity, authority: String): (UUID, Option[IndexedEntity]) = {
      val id = e.entity.unionId
      val remainingRecords = e.entity.isConflationOf.filterNot(_.sourceAuthority == authority)
      remainingRecords match {
        case Seq() => (id, None)
        case records =>
          val updated = EntityBuilder.fromRecords(records, e.entity.entityType, id)
          (id, Some(IndexedEntity(updated, e.version)))
      }
    }
    
    val entities = response.to[IndexedEntity]
    val updated = entities.map(processOneEntity(_, authority))

    // Entities that turned out empty after removing the record
    val toDelete = updated.filter(_._2.isEmpty).map(_._1)

    // Entities that had records from other sources as well, and need to be kept
    val toUpdate = updated.flatMap(_._2)

    // URIs that might be referenced by annotations
    val affectedURIs = entities.flatMap(_.entity.isConflationOf.map(_.uri))

    for {
      deleted <- deleteEntities(toDelete) // implemented by EntityService
      updated <- upsertEntities(toUpdate) // implemented by EntityService
      failedRewrites <- rewriteAnnotations(affectedURIs) // See below
    } yield (deleted && updated && failedRewrites.isEmpty)
  }
  
  /** Annotations that reference records from the given source need to have
    * the unionIds removed from their bodies.
    * 
    * This is done, once again, by running a scroll through ES. This method
    * initiates the scroll, and then hands off to processAnnotationBatch (below)
    * for the actual work.
    */
  private def rewriteAnnotations(entityUris: Seq[String]) = {
    // Shorthand to fetch next scroll result from index
    def fetch(scrollId: String) = 
      es.client execute { searchScroll(scrollId) keepAlive "5m" }
    
    // Scroll loop - hands off the actual work to processAnnotationBatch
    def scrollNext(response: RichSearchResponse, cursor: Long = 0l): Future[Seq[(Annotation, Long)]] = {
      if (response.hits.isEmpty) {
        Future.successful(Seq.empty[(Annotation, Long)])
      } else {
        processAnnotationBatch(response.to[(Annotation, Long)], entityUris).flatMap { failed =>
          val processed = cursor + response.hits.size
          if (processed < response.totalHits)
            fetch(response.scrollId).flatMap { r =>
              scrollNext(r, processed).map(_ ++ failed)
            }
          else
            Future.successful(failed)
        } 
      }
    }

    // Initial query that starts the scroll
    es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION) query boolQuery.should (
        entityUris.map { uri => termQuery("bodies.reference.uri" -> uri) }
      ) limit 50 version true scroll "5m"
    } flatMap { scrollNext(_) }
  }
  
  /** Rewrites one batch of annotations and handles retry on failure **/
  private def processAnnotationBatch(batch: Seq[(Annotation, Long)], affectedUris: Seq[String]): Future[Seq[(Annotation, Long)]] = {
    
    // Writes on batch of updated annotations to the index, handling retries
    def writeToIndex(updated: Seq[(Annotation, Long)], retries: Int = RETRIES): Future[Seq[(Annotation, Long)]] = {
      es.client.java.prepareBulk()
      es.client execute {
        bulk (
          updated.map { case (a, v) =>
            update(a.annotationId.toString) in ES.RECOGITO / ES.ANNOTATION doc a version v
          }
        )
      } map { response =>
        if (response.hasFailures)
          response.failures.map(f => Logger.warn(f.failureMessage))
          
        val failedIds = response.failures.map(_.id)      
        val failedUpdates = failedIds.map(id =>
          // Find should always deliver a return value - if it doesn't we want to fail with .get!
          updated.find(_._1.annotationId.toString == id).get)
        
        failedUpdates
      } flatMap { failed =>
        if (failed.size > 0 && retries > 0) {
          Logger.warn(s"Retrying ${failed.size} annotation updates")
          writeToIndex(failed, retries - 1)
        } else {
          if (failed.size > 0)
            Logger.error(s"Rewrite failed without recovery for ${failed.size} annotations")
          Future.successful(failed)
        }
      }
    }
    
    val updatedBatch = batch.map { case (annotation, version) =>
      val updatedAnnotation = annotation.copy(bodies = annotation.bodies.map { body =>
        body.reference.map(_.uri) match {
          case Some(uri) =>
            // Affected - remove unionId
            if (affectedUris.contains(uri)) body.copy(reference = Some(Reference(uri)))
            else body
          case None =>
            body // Body doesn't reference an entity - leave as is
        }
      })

      (updatedAnnotation, version)
    }
    
    writeToIndex(updatedBatch)
  }

}

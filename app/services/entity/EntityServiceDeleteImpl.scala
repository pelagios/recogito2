package services.entity

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.RichSearchResponse
import java.util.UUID
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.{Annotation, HasAnnotationIndexing, Reference}
import services.entity.importer.EntityBuilder
import storage.es.ES
import storage.es.HasScrollProcessing

/** Implements delete functionality for entity records.
  *
  * Due to our union index concept, entity record removal is somewhat of
  * a complex process. Therefore we split this out into a separate source
  * file.
  */
trait EntityServiceDeleteImpl extends HasScrollProcessing with HasAnnotationIndexing { self: EntityServiceImpl =>
  
  /** Rewrites one batch of annotations.
    *
    * This operation is done as a scroll, inside the (scrolling) entity batch process. I.e.
    * basically a nested loop where the "outer" iteration is the scroll through the entity
    * index, while the "inner" iteration is the scroll through the annotations for each
    * entity batch.  
    */
  private def processAnnotationBatch(affectedUris: Seq[String])(response: RichSearchResponse): Future[Boolean] = {
    val annotations = response.to[(Annotation, Long)]
    
    val updated = annotations.map { case (annotation, version) =>
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
    
    es.client.java.prepareBulk()
    es.client execute {
      bulk (
        updated.map { case (a, v) =>
          update(a.annotationId.toString) in ES.RECOGITO / ES.ANNOTATION doc a version v
        }
      )
    } map { ES.logFailures(_) } 
  }
    
  /** Annotations that reference records from the given source will remain as they are,
    * except that the unionIds will be removed from their bodies.  
    */
  private def rewriteAnnotations(uris: Seq[String]): Future[Boolean] =
    es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION) query boolQuery.should (
        uris.map { uri => termQuery("bodies.reference.uri" -> uri) }
      ) limit 50 version true scroll "5m" 
    } flatMap { scroll(processAnnotationBatch(uris), _) }
    
  /** We can safely assume that every entity contains at least one record from the given 
    * authority. First, we'll remove those. If there were are records from other sources
    * as well, we'll return the modified record. If not, we return None.
    */
  private def updateOneEntity(e: IndexedEntity, authority: String): (UUID, Option[IndexedEntity]) = {
    val id = e.entity.unionId
    val remainingRecords = e.entity.isConflationOf.filterNot(_.sourceAuthority == authority)
    remainingRecords match {
      case Seq() => (id, None)
      case records =>
        val updated = EntityBuilder.fromRecords(records, e.entity.entityType, id)
        (id, Some(IndexedEntity(updated, e.version)))
    }
  }
  
  /** Rewrites/deletes one batch of entities **/
  private def processEntityBatch(authority: String)(response: RichSearchResponse): Future[Boolean] = {
    val entities = response.to[IndexedEntity]
    val updated = entities.map(updateOneEntity(_, authority))
    
    // Entities that turned out empty after removing the record
    val toDelete = updated.filter(_._2.isEmpty).map(_._1) 
    
    // Entities that had records from other sources as well, and need to be kept
    val toUpdate = updated.flatMap(_._2)
    
    // URIs that might be referenced by annotations
    val affectedURIs = entities.flatMap(_.entity.isConflationOf.map(_.uri))
    
    for {
      deleted <- deleteEntities(toDelete)
      updated <- upsertEntities(toUpdate)
      referencesRemoved <- rewriteAnnotations(affectedURIs)
    } yield (deleted && updated && referencesRemoved)
  }
  
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
    
}
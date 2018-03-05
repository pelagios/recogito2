package services.entity.builtin.importer

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.RichSearchResponse
import javax.inject.Inject
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import services.entity.builtin.IndexedEntity
import storage.es.ES
import services.annotation.{Annotation, HasAnnotationIndexing}

class ReferenceRewriterImpl @Inject()(implicit ctx: ExecutionContext, es: ES)
  extends ReferenceRewriter with HasAnnotationIndexing {

  private val SCROLL_BATCH_SIZE = 200

  private def reindexBatch(batch: Seq[(Annotation, Long)]): Future[Boolean] = {
    if (batch.isEmpty) {
      Future.successful(true) // If there were no changes detected, this method might be handed an empty list
    } else {
      es.client execute {
        bulk(batch.map { case (annotation, version) =>
          update(annotation.annotationId.toString) in ES.RECOGITO / ES.ANNOTATION doc annotation version version
        })
      } map { ES.logFailures(_) }
    }
  }

  private def updateBatch(response: RichSearchResponse, entitiesAfter: Seq[IndexedEntity], cursor: Long = 0l): Future[Boolean] = {
    val annotations = response.to[(Annotation, Long)].toSeq
    if (annotations.isEmpty) {
      Future.successful(true)
    } else {    
      val changed = annotations.flatMap { case (before, id) =>
        val after = addUnionIds(before, entitiesAfter)

        // Don't make unnecessary updates if the union Id didn't change 
        if (after == before) None
        else Some((after, id))
      }

      reindexBatch(changed).flatMap { success =>
        val rewritten = cursor + annotations.size
        if (rewritten < response.totalHits)
          fetchNextBatch(response.scrollId).flatMap { response =>
            updateBatch(response, entitiesAfter, rewritten)
          }
        else
          Future.successful(success)
      }
    }
  }

  private def fetchNextBatch(scrollId: String): Future[RichSearchResponse] =
    es.client execute {
      searchScroll(scrollId) keepAlive "5m"
    }

  private def fetchFirstBatch(entitiesBefore: Seq[IndexedEntity], entitiesAfter: Seq[IndexedEntity]) = {
    // Potentially, changes can happen for all entity URIs, before and after
    val affectedUris = 
      (entitiesBefore ++ entitiesAfter)
        .flatMap(_.entity.isConflationOf.map(_.uri)).distinct
    
    es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION) query constantScoreQuery {
        nestedQuery("bodies") query {
          should(affectedUris.map { uri =>
            termQuery("bodies.reference.uri" -> uri)
          })
        }
      } version true limit SCROLL_BATCH_SIZE scroll "5m"
    }
  }

  override def rewriteReferencesTo(entitiesBefore: Seq[IndexedEntity], entitiesAfter: Seq[IndexedEntity]): Future[Boolean] =
    for {
      affectedAnnotations <- fetchFirstBatch(entitiesBefore, entitiesAfter)
      success <- updateBatch(affectedAnnotations, entitiesAfter)
    } yield (success)

}

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

  private def updateBatch(response: RichSearchResponse, entitiesAfter: Seq[IndexedEntity], cursor: Long = 0l): Future[Boolean] =
    response.to[(Annotation, Long)].toSeq match {
      case Nil => Future.successful(true)
      case annotationsAndIds =>
        val changed = annotationsAndIds.flatMap { case (before, id) =>
          val after = addUnionIds(before, entitiesAfter)

          // Don't make unnecessary updates if the union Id didn't change 
          if (after == before) None
          else Some((after, id))
        }

        reindexBatch(changed).flatMap { success =>
          val rewritten = cursor + annotationsAndIds.size
          if (rewritten < response.totalHits)
            fetchNextBatch(response.scrollId).flatMap { response =>
              updateBatch(response, entitiesAfter, rewritten)
            }
          else
            Future.successful(success)
        }
    }

  private def fetchNextBatch(scrollId: String): Future[RichSearchResponse] =
    es.client execute {
      searchScroll(scrollId) keepAlive "5m"
    }

  private def fetchFirstBatch(referencedEntities: Seq[IndexedEntity]) =
    es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION) query constantScoreQuery {
        should(referencedEntities.map { e =>
          termQuery("bodies.reference.union_id" -> e.entity.unionId.toString)
        })
      } version true limit SCROLL_BATCH_SIZE scroll "5m"
    }

  override def rewriteReferencesTo(entitiesBefore: Seq[IndexedEntity], entitiesAfter: Seq[IndexedEntity]): Future[Boolean] =
    if (entitiesBefore.isEmpty)
      Future.successful(true)
    else
      for {
        affectedAnnotations <- fetchFirstBatch(entitiesBefore)
        success <- updateBatch(affectedAnnotations, entitiesAfter)
      } yield (success)

}

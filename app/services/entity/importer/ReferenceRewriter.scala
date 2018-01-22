package services.entity.importer

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.RichSearchResponse
import scala.concurrent.{ExecutionContext, Future}
import services.entity.IndexedEntity
import storage.ES
import services.annotation.{Annotation, HasAnnotationIndexing}

trait ReferenceRewriter extends HasAnnotationIndexing {

  private val SCROLL_BATCH_SIZE = 200

  private def reindexBatch(batch: Seq[(Annotation, Long)])(implicit es: ES, ctx: ExecutionContext): Future[Boolean] =
    es.client execute {
      bulk(batch.map { case (annotation, version) =>
        update(annotation.annotationId.toString) in ES.RECOGITO / ES.ANNOTATION doc annotation version version
      })
    } map { _.hasFailures }

  private def updateBatch(response: RichSearchResponse, cursor: Long = 0l)(implicit es: ES, ctx: ExecutionContext): Future[Boolean] =
    response.to[(Annotation, Long)].toSeq match {
      case Nil => Future.successful(true)
      case annotationsAndIds =>
        val updated = annotationsAndIds.map { case (a, id) =>
          // TODO update union_id in annotation bodies
          (a, id)
        }

        reindexBatch(updated).flatMap { success =>
          val rewritten = cursor + annotationsAndIds.size
          if (rewritten < response.totalHits)
            fetchNextBatch(response.scrollId).flatMap { response =>
              updateBatch(response, rewritten)
            }
          else
            Future.successful(success)
        }
    }

  private def fetchNextBatch(scrollId: String)(implicit es: ES): Future[RichSearchResponse] =
    es.client execute {
      searchScroll(scrollId) keepAlive "5m"
    }

  private def fetchFirstBatch(referencedEntities: Seq[IndexedEntity])(implicit es: ES) =
    es.client execute {
      search(ES.RECOGITO / ES.ANNOTATION) query constantScoreQuery {
        should(referencedEntities.map { e =>
          termQuery("bodies.reference.union_id" -> e.entity.unionId.toString)
        })
      } limit SCROLL_BATCH_SIZE scroll "5m"
    }

  def rewriteReferencesTo(entitiesBefore: Seq[IndexedEntity], entitiesAfter: Seq[IndexedEntity])(implicit es: ES, ctx: ExecutionContext): Future[Boolean] =
    if (entitiesBefore.isEmpty)
      Future.successful(true)
    else
      for {
        affectedAnnotations <- fetchFirstBatch(entitiesBefore)
        success <- updateBatch(affectedAnnotations)
      } yield (success)

}

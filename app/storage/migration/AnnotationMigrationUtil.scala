package storage.migration

import com.sksamuel.elastic4s.{Hit, HitReader}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.RichSearchResponse
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.HasAnnotationIndexing
import services.entity.EntityService
import storage.migration.legacy.LegacyAnnotation
import storage.ES

class AnnotationMigrationUtil @Inject()(
  entityService: EntityService,
  implicit val ctx: ExecutionContext,
  implicit val es: ES
) extends HasAnnotationIndexing {

  implicit object LegacyAnnotationHitReader extends HitReader[LegacyAnnotation] {
    override def read(hit: Hit): Either[Throwable, LegacyAnnotation] =
      Right(Json.fromJson[LegacyAnnotation](Json.parse(hit.sourceAsString)).get)
  }

  private val SOURCE_INDEX_LEGACY = "recogito"
  
  private val DEST_INDEX_NEW = "recogito-new"
  
  private def fetchNextBatch(scrollId: String): Future[RichSearchResponse] =
      es.client execute { searchScroll(scrollId) keepAlive "5m" }
  
  private def reindex(hits: Seq[LegacyAnnotation]): Future[Boolean] = ???
  
  private def migrateBatch(response: RichSearchResponse, cursor: Long = 0l): Future[Boolean] =
    if (response.hits.isEmpty) {
      Future.successful(true)
    } else {
      reindex(response.to[LegacyAnnotation]).flatMap { success =>
        val reindexedAnnotations = cursor + response.hits.size
        if (reindexedAnnotations < response.totalHits)
          fetchNextBatch(response.scrollId).flatMap(migrateBatch(_, reindexedAnnotations).map(_ && success))
        else
          Future.successful(success)
      }
    }
  
  private def fetchFirstBatch =
    es.client execute {
      search(SOURCE_INDEX_LEGACY / ES.ANNOTATION) query matchAllQuery limit 200 scroll "5m"
    } flatMap { migrateBatch(_) } map { success =>
      
    }
  
}
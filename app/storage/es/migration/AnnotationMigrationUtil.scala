package storage.es.migration

import com.sksamuel.elastic4s.{Hit, HitReader}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.RichSearchResponse
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.AnnotationService
import services.entity.builtin.EntityService
import storage.es.{ES, HasScrollProcessing}

class AnnotationMigrationUtil @Inject()(
  annotationService: AnnotationService,
  entityService: EntityService,
  implicit val ctx: ExecutionContext,
  implicit val es: ES
) extends HasScrollProcessing {

  implicit object LegacyAnnotationHitReader extends HitReader[LegacyAnnotation] {
    override def read(hit: Hit): Either[Throwable, LegacyAnnotation] =
      Right(Json.fromJson[LegacyAnnotation](Json.parse(hit.sourceAsString)).get)
  }

  private val SOURCE_INDEX_LEGACY = "recogito_v22"

  private val DEST_INDEX_NEW = "recogito"

  private def reindex(response: RichSearchResponse): Future[Boolean] = {
    val hits = response.to[LegacyAnnotation]
    annotationService.upsertAnnotations(
      annotations = hits.map(_.toNewAPI),
      versioned = false
    ).map(_.isEmpty)
  }

  def runMigration: Future[Boolean] =
    es.client execute {
      search(SOURCE_INDEX_LEGACY / ES.ANNOTATION) query matchAllQuery limit 200 scroll "5m"
    } flatMap { scroll(reindex, _) } map { success =>
      if (success) play.api.Logger.info("Migration completed successfully. Yay!")
      else play.api.Logger.info("Migration stopped. Something went wrong.")
      success
    }

}

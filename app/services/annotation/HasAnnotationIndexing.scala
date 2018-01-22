package services.annotation

import com.sksamuel.elastic4s.{HitReader, Indexable, Hit}
import play.api.libs.json.Json
import scala.util.Try
import services.HasTryToEither

/** Common annotation de/serialization code for indexing.
  *  
  * Annotations are written and read from the index not just in the
  * AnnotationService, but also in the AnnotationHistoryService and 
  * by the ReferenceRewriter, as part of entity import. Therefore
  * we extract this into a re-usable trait.
  */
trait HasAnnotationIndexing {

  implicit object AnnotationIndexable extends Indexable[Annotation] {
    override def json(a: Annotation): String = Json.stringify(Json.toJson(a))
  }

  implicit object AnnotationHitReader extends HitReader[(Annotation, Long)] with HasTryToEither {
    override def read(hit: Hit): Either[Throwable, (Annotation, Long)] =
      Try(Json.fromJson[Annotation](Json.parse(hit.sourceAsString)).get, hit.version)
  }

}
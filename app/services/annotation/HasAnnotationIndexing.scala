package services.annotation

import com.sksamuel.elastic4s.{HitReader, Indexable, Hit}
import java.util.UUID
import play.api.libs.json.Json
import scala.util.Try
import services.HasTryToEither
import services.entity.builtin.IndexedEntity

/** Common annotation de/serialization code for indexing.
  *  
  * Annotations are written and read from the index not just in the
  * AnnotationService, but also in the AnnotationHistoryService and 
  * by the ReferenceRewriter, as part of entity import. Therefore
  * we extract this into a re-usable trait.
  */
trait HasAnnotationIndexing {
  
  // Frontent serialization format
  import services.annotation.BackendAnnotation._

  implicit object AnnotationIndexable extends Indexable[Annotation] {
    override def json(a: Annotation): String = Json.stringify(Json.toJson(a))
  }

  implicit object AnnotationHitReader extends HitReader[(Annotation, Long)] with HasTryToEither {
    override def read(hit: Hit): Either[Throwable, (Annotation, Long)] =
      Try(Json.fromJson[Annotation](Json.parse(hit.sourceAsString)).get, hit.version)
  }
  
  private def copyWithUnionId(body: AnnotationBody, unionId: Option[UUID]) =
    body.copy(reference = body.reference.map(_.copy(unionId = unionId)))
  
  def addUnionIds(annotation: Annotation, resolvedIndexed: Seq[IndexedEntity]) = {
    val resolved = resolvedIndexed.map(_.entity)
    annotation.copy(bodies = annotation.bodies.map { body =>
      val referencedEntity = body.uri.flatMap(uri => resolved.find(_.uris.contains(uri)))      
      referencedEntity match {
        // No referenced entity - remove unionId (if any)
        case None => copyWithUnionId(body, None)
        // Found referenced entity - set unionId accordingly
        case Some(e) => copyWithUnionId(body, Some(e.unionId))
      }
    })
  }

}
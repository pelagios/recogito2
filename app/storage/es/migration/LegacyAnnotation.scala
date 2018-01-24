package storage.es.migration

import java.util.UUID
import services.{ContentType, HasContentTypeList, HasDate}
import services.annotation.{Annotation, AnnotatedObject}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class LegacyAnnotation(
  annotationId: UUID,
  versionId: UUID,
  annotates: AnnotatedObject,
  contributors: Seq[String],
  anchor: String,
  lastModifiedBy: Option[String],
  lastModifiedAt: DateTime,
  bodies: Seq[LegacyAnnotationBody]
) {

  def toNewAPI = Annotation(
    annotationId,
    versionId,
    annotates,
    contributors,
    anchor,
    lastModifiedBy,
    lastModifiedAt,
    bodies.map(_.toNewAPI)
  )

}

object LegacyAnnotation extends HasDate {

  implicit val legacyAnnotationFormat: Format[LegacyAnnotation] = (
    (JsPath \ "annotation_id").format[UUID] and
    (JsPath \ "version_id").format[UUID] and
    (JsPath \ "annotates").format[AnnotatedObject] and
    (JsPath \ "contributors").format[Seq[String]] and
    (JsPath \ "anchor").format[String] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime] and
    (JsPath \ "bodies").format[Seq[LegacyAnnotationBody]]
  )(LegacyAnnotation.apply, unlift(LegacyAnnotation.unapply))

}

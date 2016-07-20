package models.annotation

import java.util.UUID
import models.{ ContentType, HasContentTypeList, HasDate }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Annotation(

  annotationId: UUID,

  versionId: UUID,

  annotates: AnnotatedObject,

  contributors: Seq[String],

  anchor: String,

  lastModifiedBy: Option[String],

  lastModifiedAt: DateTime,

  bodies: Seq[AnnotationBody]

)

case class AnnotatedObject(documentId: String, filepartId: UUID, contentType: ContentType)

object AnnotatedObject extends HasContentTypeList {

  implicit val annotatedObjectFormat: Format[AnnotatedObject] = (
    (JsPath \ "document_id").format[String] and
    (JsPath \ "filepart_id").format[UUID] and
    (JsPath \ "content_type").format[JsValue].inmap[ContentType](fromCTypeList, toCTypeList)
  )(AnnotatedObject.apply, unlift(AnnotatedObject.unapply))

}

object Annotation extends HasDate {

  implicit val annotationFormat: Format[Annotation] = (
    (JsPath \ "annotation_id").format[UUID] and
    (JsPath \ "version_id").format[UUID] and
    (JsPath \ "annotates").format[AnnotatedObject] and
    (JsPath \ "contributors").format[Seq[String]] and
    (JsPath \ "anchor").format[String] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime] and
    (JsPath \ "bodies").format[Seq[AnnotationBody]]
  )(Annotation.apply, unlift(Annotation.unapply))

}

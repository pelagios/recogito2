package models.annotation

import java.util.UUID
import models.HasDate
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Annotation(

  annotationId: UUID,

  versionId: UUID,

  annotates: AnnotatedObject,

  hasPreviousVersions: Option[Int],

  contributors: Seq[String],

  anchor: String,

  lastModifiedBy: Option[String],

  lastModifiedAt: DateTime,

  bodies: Seq[AnnotationBody],

  status: AnnotationStatus

)

case class AnnotatedObject(document: String, filepart: Int)

object AnnotatedObject {

  /** JSON conversion **/
  implicit val annotatedObjectFormat: Format[AnnotatedObject] = (
    (JsPath \ "document").format[String] and
    (JsPath \ "filepart").format[Int]
  )(AnnotatedObject.apply, unlift(AnnotatedObject.unapply))

}

object Annotation extends HasDate {

  /** JSON conversion **/
  implicit val annotationFormat: Format[Annotation] = (
    (JsPath \ "_id").format[UUID] and
    (JsPath \ "version_id").format[UUID] and
    (JsPath \ "annotates").format[AnnotatedObject] and
    (JsPath \ "has_previous_versions").formatNullable[Int] and
    (JsPath \ "contributors").format[Seq[String]] and
    (JsPath \ "anchor").format[String] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime] and
    (JsPath \ "bodies").format[Seq[AnnotationBody]] and
    (JsPath \ "status").format[AnnotationStatus]
  )(Annotation.apply, unlift(Annotation.unapply))

}

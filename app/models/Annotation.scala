package models

import java.util.{ Date, UUID }
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import javax.json.JsonNumber

case class Annotation(

  annotationId: UUID,

  versionId: UUID,

  annotates: AnnotatedObject,

  hasPreviousVersions: Option[Int],

  contributors: Seq[String],

  anchor: String,

  createdBy: Option[String],

  createdAt: Date,

  lastModifiedBy: Option[String],

  lastModifiedAt: Date,

  bodies: Seq[AnnotationBody],

  status: AnnotationStatus

)

case class AnnotatedObject(document: Int, filepart: Int)

object Annotation {

  /** JSON conversion **/
  implicit val annotatedObjectFormat: Format[AnnotatedObject] = (
    (JsPath \ "document").format[Int] and
    (JsPath \ "filepart").format[Int]
  )(AnnotatedObject.apply, unlift(AnnotatedObject.unapply))

  implicit val annotationFormat: Format[Annotation] = (
    (JsPath \ "annotation_id").format[UUID] and
    (JsPath \ "version_id").format[UUID] and
    (JsPath \ "annotates").format[AnnotatedObject] and
    (JsPath \ "has_previous_versions").formatNullable[Int] and 
    (JsPath \ "contributors").format[Seq[String]] and
    (JsPath \ "anchor").format[String] and
    (JsPath \ "created_by").formatNullable[String] and
    (JsPath \ "created_at").format[Date] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[Date] and
    (JsPath \ "bodies").format[Seq[AnnotationBody]] and
    (JsPath \ "status").format[AnnotationStatus]  
  )(Annotation.apply, unlift(Annotation.unapply))
  
}

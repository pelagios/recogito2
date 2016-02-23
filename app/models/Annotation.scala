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
  
  /** JSON serialization **/ 
  implicit val annotationWrites: Writes[Annotation] = (
    (JsPath \ "annotation_id").write[String] ~
    (JsPath \ "version_id").write[String] ~
    (JsPath \ "annotates").write[JsValue] ~
    (JsPath \ "has_previous_versions").writeNullable[Int] ~
    (JsPath \ "contributors").write[Seq[String]] ~
    (JsPath \ "anchor").write[String] ~
    (JsPath \ "created_by").writeNullable[String] ~
    (JsPath \ "created_at").write[Date] ~
    (JsPath \ "last_modified_by").writeNullable[String] ~
    (JsPath \ "last_modified_at").write[Date] ~
    (JsPath \ "bodies").write[Seq[AnnotationBody]] ~
    (JsPath \ "status").write[AnnotationStatus]
  )(a => (
      a.annotationId.toString,
      a.versionId.toString,
      Json.obj("document" -> a.annotates.document, "filepart" -> a.annotates.filepart),
      a.hasPreviousVersions,
      a.contributors,
      a.anchor,
      a.createdBy,
      a.createdAt,
      a.lastModifiedBy,
      a.lastModifiedAt,
      a.bodies,
      a.status))
  
}

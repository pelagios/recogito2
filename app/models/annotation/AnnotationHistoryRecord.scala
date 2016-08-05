package models.annotation

import java.util.UUID
import models.HasDate
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import models.HasNullableSeq

case class AnnotationHistoryRecord (
    
  annotationId: UUID,
  
  versionId: UUID,

  annotates: AnnotatedObject,

  contributors: Seq[String],

  anchor: String,
  
  lastModifiedBy: Option[String],

  lastModifiedAt: DateTime,
  
  bodies: Seq[AnnotationBody],
  
  deleted: Boolean
      
) {
  
  /** This method will fail for delete markers **/
  def asAnnotation = Annotation(
    annotationId,
    versionId,
    annotates,
    contributors,
    anchor,
    lastModifiedBy,
    lastModifiedAt,
    bodies)
  
}

object AnnotationHistoryRecord extends HasDate with HasNullableSeq {
  
  protected def fromOptBoolean(o: Option[Boolean]) =
    o.getOrElse(false)

  protected def toOptBoolean(b: Boolean) =
    if (b == false) None else Some(true)
 
  implicit val annotationHistoryFormat: Format[AnnotationHistoryRecord] = (
    (JsPath \ "annotation_id").format[UUID] and
    (JsPath \ "version_id").format[UUID] and
    (JsPath \ "annotates").format[AnnotatedObject] and
    (JsPath \ "contributors").formatNullable[Seq[String]]
      .inmap(fromOptSeq[String], toOptSeq[String]) and
    (JsPath \ "anchor").format[String] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime] and
    (JsPath \ "bodies").formatNullable[Seq[AnnotationBody]]
      .inmap(fromOptSeq[AnnotationBody], toOptSeq[AnnotationBody]) and
    (JsPath \ "deleted").formatNullable[Boolean]
      .inmap[Boolean](fromOptBoolean, toOptBoolean) 
  )(AnnotationHistoryRecord.apply, unlift(AnnotationHistoryRecord.unapply))
  
  def forVersion(a: Annotation) = AnnotationHistoryRecord(
    a.annotationId,
    a.versionId,
    a.annotates,
    a.contributors,
    a.anchor,
    a.lastModifiedBy,
    a.lastModifiedAt,
    a.bodies,
    false)
    
  def forDelete(a: Annotation, deletedBy: String, deletedAt: DateTime) = AnnotationHistoryRecord(
    a.annotationId,
    UUID.randomUUID, // Delete markers get their own version ID
    a.annotates, 
    a.contributors,
    a.anchor,
    Some(deletedBy),
    deletedAt, 
    a.bodies,
    true)
  
}

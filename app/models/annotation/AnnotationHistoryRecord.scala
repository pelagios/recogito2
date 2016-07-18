package models.annotation

import java.util.UUID
import models.HasDate
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AnnotationHistoryRecord (
    
  annotationId: UUID,

  annotates: AnnotatedObject,
  
  lastModifiedBy: Option[String],

  lastModifiedAt: DateTime,
  
  deleted: Boolean
      
)

object AnnotationHistoryRecord extends HasDate {
  
  protected def fromOptBoolean(o: Option[Boolean]) =
    o.getOrElse(false)

  protected def toOptBoolean(b: Boolean) =
    if (b == false) None else Some(true)
 
  implicit val annotationHistoryFormat: Format[AnnotationHistoryRecord] = (
    (JsPath \ "annotation_id").format[UUID] and
    (JsPath \ "annotates").format[AnnotatedObject] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime] and
    (JsPath \ "deleted").formatNullable[Boolean]
      .inmap[Boolean](fromOptBoolean, toOptBoolean) 
  )(AnnotationHistoryRecord.apply, unlift(AnnotationHistoryRecord.unapply))
  
  def forVersion(a: Annotation) =
    AnnotationHistoryRecord(a.annotationId, a.annotates, a.lastModifiedBy, a.lastModifiedAt, false)
    
  def forDelete(a: Annotation, deletedBy: String, deletedAt: DateTime) =
    AnnotationHistoryRecord(a.annotationId, a.annotates, Some(deletedBy), deletedAt, true)
  
}

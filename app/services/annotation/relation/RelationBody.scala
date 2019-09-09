package services.annotation.relation

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasDate
import services.annotation.AnnotationBody

case class RelationBody(
  hasType: AnnotationBody.Type,
  lastModifiedBy: Option[String],
  lastModifiedAt: DateTime,
  value: String
) {

  def equalsIgnoreModified(other: AnnotationBody) =
    hasType == other.hasType && value == other.value

}
  
object RelationBody extends HasDate {
  
  implicit val relationBodyFormat: Format[RelationBody] = (
    (JsPath \ "type").format[AnnotationBody.Value] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime] and
    (JsPath \ "value").format[String]
  )(RelationBody.apply, unlift(RelationBody.unapply))
  
}
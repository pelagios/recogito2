package services.annotation

import services.HasDate
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class AnnotationBody (
  hasType: AnnotationBody.Type,
  lastModifiedBy: Option[String],
  lastModifiedAt: DateTime,
  value: Option[String],
  reference: Option[Reference],
  note: Option[String], 
  status: Option[AnnotationStatus]
) {
  
  /** For convenience: shorthand to entity ref URI **/
  lazy val uri = reference.map(_.uri)
  
  /** Returns true if the bodies are equal in terms of content.
    *
    * Instead of the standard .equals method, this will only check
    * hasType, value, uri and status fields, but not modifiedBy and
    * modifiedAt. This way we can ignore empty edits that didn't 
    * actually change anything.
    */
  def equalsIgnoreModified(other: AnnotationBody) = {
    val isStatusEqual = (status, other.status) match {
      case (Some(a), Some(b)) => a.equalsIgnoreModified(b)
      case (None, None) => true
      case _ => false
    }
    
    if (!isStatusEqual)
      false
    else if (hasType != other.hasType)
      false
    else if (value != other.value)
      false
    else if (reference.map(_.uri) != other.reference.map(_.uri))
      false
    else
      true
  }
  
}

object AnnotationBody extends Enumeration with HasDate {

  type Type = Value

  val COMMENT = Value("COMMENT")

  val PERSON = Value("PERSON")

  val PLACE = Value("PLACE")
  
  val EVENT = Value("EVENT")

  val QUOTE = Value("QUOTE")

  val TAG = Value("TAG")

  val TRANSCRIPTION = Value("TRANSCRIPTION")

  implicit val annotationBodyTypeFormat: Format[AnnotationBody.Type] =
    Format(
      JsPath.read[String].map(AnnotationBody.withName(_)),
      Writes[AnnotationBody.Type](t => JsString(t.toString))
    )

  implicit val annotationBodyFormat: Format[AnnotationBody] = (
    (JsPath \ "type").format[AnnotationBody.Value] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime] and
    (JsPath \ "value").formatNullable[String] and
    (JsPath \ "reference").formatNullable[Reference] and
    (JsPath \ "note").formatNullable[String] and
    (JsPath \ "status").formatNullable[AnnotationStatus]
  )(AnnotationBody.apply, unlift(AnnotationBody.unapply))

}
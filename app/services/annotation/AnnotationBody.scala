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

object AnnotationBody extends Enumeration {

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
    
  def quoteBody(quote: String) = 
    AnnotationBody(
      AnnotationBody.QUOTE,
      None, // lastModifiedBy
      DateTime.now(),
      Some(quote),
      None, // reference
      None, // note
      None) // status

  private def entityBody(hasType: AnnotationBody.Type, reference: Option[String] = None) = 
    AnnotationBody(
      hasType,
      None, // lastModifiedBy
      DateTime.now(),
      None, // value
      reference,
      None, // note
      None) // status
  
  def placeBody(reference: Option[String] = None) = 
    entityBody(AnnotationBody.PLACE, reference)
      
  def personBody(reference: Option[String] = None) = 
    entityBody(AnnotationBody.PERSON, reference)

  def tagBody(tag: String) =
    AnnotationBody(
      AnnotationBody.TAG,
      None,
      DateTime.now(),
      Some(tag),
      None,
      None,
      None)
}

/** Backend/ES serialization includes the union Id (which is for internal use only) **/
object BackendAnnotationBody extends HasDate {

  implicit val backendAnnotationBodyFormat: Format[AnnotationBody] = (
    (JsPath \ "type").format[AnnotationBody.Value] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime] and
    (JsPath \ "value").formatNullable[String] and
    (JsPath \ "reference").formatNullable[Reference] and
    (JsPath \ "note").formatNullable[String] and
    (JsPath \ "status").formatNullable[AnnotationStatus]
  )(AnnotationBody.apply, unlift(AnnotationBody.unapply))

}

/** Frontend serialization includes only URIs **/
object FrontendAnnotationBody extends HasDate {

  implicit val frontendAnnotationBodyWrites: Writes[AnnotationBody] = (
    (JsPath \ "type").write[AnnotationBody.Value] and
    (JsPath \ "last_modified_by").writeNullable[String] and
    (JsPath \ "last_modified_at").write[DateTime] and
    (JsPath \ "value").writeNullable[String] and
    (JsPath \ "uri").writeNullable[String] and
    (JsPath \ "note").writeNullable[String] and
    (JsPath \ "status").writeNullable[AnnotationStatus]
  )(b => (
      b.hasType,
      b.lastModifiedBy,
      b.lastModifiedAt,
      b.value,
      b.reference.map(_.uri),
      b.note,
      b.status))

}


package controllers.document.downloads.serializers.annotations.webannotation

import services.HasDate
import services.annotation.AnnotationBody
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class WebAnnotationBody(
  hasType : Option[WebAnnotationBody.Value],
  id      : Option[String], 
  value   : Option[String],
  note    : Option[String],
  creator : Option[String],
  modified: DateTime,
  purpose : Option[String]
)
  
object WebAnnotationBody extends Enumeration with HasDate {
  
  val TextualBody = Value("TextualBody")
  
  /** Note we don't explicitely serialize QUOTE bodies **/
  def fromAnnotationBody(b: AnnotationBody, recogitoBaseURI: String): Option[WebAnnotationBody] = {
    
    import AnnotationBody._
    
    val hasType = b.hasType match {
      case COMMENT | TAG | QUOTE | TRANSCRIPTION => Some(TextualBody)
      case _ => None
    }
    
    val purpose = b.hasType match {
      case COMMENT        => Some("commenting")
      case TRANSCRIPTION  => Some("transcribing")
      case TAG            => Some("tagging")
      case PLACE | PERSON => Some("identifying")
      case _ => None
    }
    
    if (b.hasType == QUOTE)
      None
    else
      Some(WebAnnotationBody(
        hasType,
        b.uri,
        b.value,
        b.note,
        b.lastModifiedBy.map(by => recogitoBaseURI + by),
        b.lastModifiedAt,
        purpose))
  }
  
  implicit val webAnnotationBodyWrites: Writes[WebAnnotationBody] = (
    (JsPath \ "type").writeNullable[WebAnnotationBody.Value] and
    (JsPath \ "id").writeNullable[String] and
    (JsPath \ "value").writeNullable[String] and
    (JsPath \ "note").writeNullable[String] and
    (JsPath \ "creator").writeNullable[String] and
    (JsPath \ "modified").write[DateTime] and
    (JsPath \ "purpose").writeNullable[String]
  )(unlift(WebAnnotationBody.unapply))
  
}
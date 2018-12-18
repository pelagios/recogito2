package controllers.document.downloads.serializers.annotations.webannotation

import services.HasDate
import services.annotation.AnnotationBody
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class WebAnnotationBody(
  hasType : WebAnnotationBody.Value,
  value   : Option[String],
  note    : Option[String],
  creator : Option[String],
  modified: DateTime,
  purpose : Option[String]
)
  
object WebAnnotationBody extends Enumeration with HasDate {
  
  val TextualBody = Value("TextualBody")

  val SpecificResource = Value("SpecificResource")
  
  /** Note we don't explicitely serialize QUOTE bodies **/
  def fromAnnotationBody(b: AnnotationBody, recogitoBaseURI: String): Option[WebAnnotationBody] = {
    
    import AnnotationBody._
    
    val hasType = b.hasType match {
      case COMMENT | TAG | QUOTE | TRANSCRIPTION => TextualBody
      case _ => SpecificResource
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
        Seq(b.uri, b.value).flatten.headOption,
        b.note,
        b.lastModifiedBy.map(by => recogitoBaseURI + by),
        b.lastModifiedAt,
        purpose))
  }
  
  implicit val webAnnotationBodyWrites: Writes[WebAnnotationBody] = (
    (JsPath \ "type").write[WebAnnotationBody.Value] and
    (JsPath \ "value").writeNullable[String] and
    (JsPath \ "note").writeNullable[String] and
    (JsPath \ "creator").writeNullable[String] and
    (JsPath \ "modified").write[DateTime] and
    (JsPath \ "purpose").writeNullable[String]
  )(unlift(WebAnnotationBody.unapply))
  
}
package controllers.document.downloads.serializers.annotations.webannotation

import com.vividsolutions.jts.geom.Geometry
import services.{HasDate, HasGeometry}
import services.annotation.AnnotationBody
import services.entity.Entity
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class WebAnnotationBody(
  hasType : WebAnnotationBody.Value,
  value   : Option[String],
  id      : Option[String],
  note    : Option[String],
  creator : Option[String],
  modified: DateTime,
  purpose : Option[String],
  entity  : Option[Entity]
)

  
object WebAnnotationBody extends Enumeration with HasDate with HasGeometry {
  
  val TextualBody = Value("TextualBody")

  val SpecificResource = Value("SpecificResource")

  val Feature = Value("Feature")
  
  /** Note we don't explicitely serialize QUOTE bodies **/
  def fromAnnotationBody(b: AnnotationBody, recogitoBaseURI: String, entities: Seq[Entity]): Seq[WebAnnotationBody] = {
    
    import AnnotationBody._
    
    val hasType = b.hasType match {
      case COMMENT | TAG | QUOTE | TRANSCRIPTION | ENTITY | LABEL | SYMBOL | GROUPING | COLORING => TextualBody
      case _ => SpecificResource
    }
    
    val purpose = b.hasType match {
      case COMMENT        => Some("commenting")
      case TRANSCRIPTION  => Some("transcribing")
      case TAG            => Some("tagging")
      case PLACE | PERSON => Some("identifying")
      case ENTITY | LABEL | SYMBOL => Some("classifying")
      case GROUPING       => Some("grouping")
      case ORDERING       => Some("ordering")
      case COLORING       => Some("coloring")
      case _ => None
    }
    
    if (b.hasType == QUOTE) {
      Seq.empty[WebAnnotationBody]
    } else {
      val (value, id) = if (b.value.isDefined && b.uri.isDefined) {
          (b.value, b.uri) 
        } else { 
          (Seq(b.uri, b.value, Option(b.hasType.toString)).flatten.headOption, None)
        }

      val body = WebAnnotationBody(
        hasType,
        value,
        id,
        b.note,
        b.lastModifiedBy.map(by => recogitoBaseURI + by),
        b.lastModifiedAt,
        purpose,
        None)

      val maybeGeoReference =
        if (b.hasType == PLACE)
          entities.map { e => WebAnnotationBody(
            Feature,
            None,
            None,
            None,
            b.lastModifiedBy.map(by => recogitoBaseURI + by),
            b.lastModifiedAt,
            Some("georeferencing"),
            Some(e)
          )}
        else 
          Seq.empty[WebAnnotationBody]

      body +: maybeGeoReference
    }
  }
  
  implicit val webAnnotationBodyWrites: Writes[WebAnnotationBody] = (
    (JsPath \ "type").write[WebAnnotationBody.Value] and
    (JsPath \ "id").writeNullable[String] and
    (JsPath \ "value").writeNullable[String] and
    (JsPath \ "note").writeNullable[String] and
    (JsPath \ "creator").writeNullable[String] and
    (JsPath \ "modified").write[DateTime] and
    (JsPath \ "purpose").writeNullable[String] and
    (JsPath \ "geometry").writeNullable[Geometry]
  )(a => (
    a.hasType,
    a.id,
    a.value,
    a.note,
    a.creator,
    a.modified,
    a.purpose,
    a.entity.flatMap(_.representativeGeometry)
  ))
  
}
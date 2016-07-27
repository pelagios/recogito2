package models.geotag

import java.util.UUID
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/** GeoTag is an associative entity that creates a many-to-many relationship between places and annotations **/
case class GeoTag (
  
  /** ID of the place this link is attached to via parent relationship **/ 
  placeId: String,
  
  /** UUID of the annotation this link is part of **/
  annotationId: UUID,
  
  /** ID of the document the annotation annotates **/ 
  documentId: String,
  
  /** The ID of the filepart within the document which the annotation annotates **/
  filepartId: UUID,
  
  /** The gazetteer URI used in the annotation to point to the place **/
  gazetteerUri: String
  
)

object GeoTag {
  
  implicit val placeLinkFormat: Format[GeoTag] = (
    (JsPath \ "place_id").format[String] and
    (JsPath \ "annotation_id").format[UUID] and
    (JsPath \ "document_id").format[String] and
    (JsPath \ "filepart_id").format[UUID] and
    (JsPath \ "gazetteer_uri").format[String]
  )(GeoTag.apply, unlift(GeoTag.unapply))
  
}

package models.place

import java.util.UUID
import models.{ HasNullableSeq, HasDate }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class PlaceLink(
  
  /** ID of the place this link is attached to via parent relationship **/ 
  placeId: String,
  
  /** UUID of the annotation this link is part of **/
  annotationId: UUID,
  
  /** ID of the document the annotation annotates **/ 
  documentId: String,
  
  /** The ID of the filepart within the document which the annotation annotates **/
  filepartId: Int,
  
  /** The gazetteer URI used in the annotation to point to the place **/
  gazetteerUri: String
  
)

/** JSON (de)serialization **/

object PlaceLink {
  
  implicit val placeLinkFormat: Format[PlaceLink] = (
    (JsPath \ "place_id").format[String] and
    (JsPath \ "annotation_id").format[UUID] and
    (JsPath \ "document_id").format[String] and
    (JsPath \ "filepart_id").format[Int] and
    (JsPath \ "gazetteer_uri").format[String]
  )(PlaceLink.apply, unlift(PlaceLink.unapply))
  
}

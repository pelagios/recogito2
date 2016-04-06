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
  gazetteerUri: String,
  
  /** List of users contributing to the annotation this link is part of **/
  contributors: Seq[String],
  
  lastModifiedBy: Option[String],

  lastModifiedAt: DateTime
  
)

/** JSON (de)serialization **/

object PlaceLink extends HasNullableSeq with HasDate {
  
  implicit val placeLinkFormat: Format[PlaceLink] = (
    (JsPath \ "_parent").format[String] and
    (JsPath \ "annotation_id").format[UUID] and
    (JsPath \ "document_id").format[String] and
    (JsPath \ "filepart_id").format[Int] and
    (JsPath \ "gazetteer_uri").format[String] and
    (JsPath \ "contributors").formatNullable[Seq[String]]
      .inmap[Seq[String]](fromOptSeq[String], toOptSeq[String]) and    
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime]
  )(PlaceLink.apply, unlift(PlaceLink.unapply))
  
}

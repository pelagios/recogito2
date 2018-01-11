package services.geotag

import java.util.UUID
import services.{ HasDate, HasNullableSeq }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/** GeoTag is an associative entity that creates a many-to-many relationship between places and annotations **/
case class GeoTag (
  
  /** UUID of the annotation this link is part of **/
  annotationId: UUID,
  
  /** ID of the document the annotation annotates **/ 
  documentId: String,
  
  /** The ID of the filepart within the document which the annotation annotates **/
  filepartId: UUID,
  
  /** The gazetteer URI used in the annotation to point to the place **/
  gazetteerUri: String,
  
  /** The transcription or quote, if any (could be empty on untranscribed images) **/
  toponyms: Seq[String],
  
  contributors: Seq[String],
  
  lastModifiedBy: Option[String],
  
  lastModifiedAt: DateTime
  
)

object GeoTag extends HasNullableSeq with HasDate {
  
  implicit val placeLinkFormat: Format[GeoTag] = (
    (JsPath \ "annotation_id").format[UUID] and
    (JsPath \ "document_id").format[String] and
    (JsPath \ "filepart_id").format[UUID] and
    (JsPath \ "gazetteer_uri").format[String] and
    (JsPath \ "toponyms").formatNullable[Seq[String]]
      .inmap[Seq[String]](fromOptSeq[String], toOptSeq[String]) and
    (JsPath \ "contributors").formatNullable[Seq[String]]
      .inmap[Seq[String]](fromOptSeq[String], toOptSeq[String]) and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime]
  )(GeoTag.apply, unlift(GeoTag.unapply))
  
}

package models.place.crosswalks

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import models.place.GazetteerRecord
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import models.place.Name
import models.HasGeometry

object PleiadesCrosswalk {
  
  def fromJson(record: String): GazetteerRecord = ??? // TODO implement
  
}

case class Feature(geometry: Geometry)

object Feature extends HasGeometry {
  
  implicit val featureReads: Reads[Feature] =
    (JsPath \ "geometry").read[Geometry].map(Feature(_))
  
}

case class PleiadesRecord(

  uri: String,

  // TODO lastChange
  
  title: String,
  
  description: Option[String],
  
  names: Seq[Name],
  
  features: Seq[Feature],
  
  representativePoint: Option[Coordinate],
  
  // TODO temporal bounds
  
  placeTypes: Seq[String]
  
  // TODO close matches
  
  // TODO exact matches
  
  // TODO include modernCountry for "Pleiades-like" gazetteers?
    
)

object PleiadesRecord extends HasGeometry {
  
  implicit val pleiadesRecordReads: Reads[PleiadesRecord] = (
    (JsPath \ "uri").read[String] and
    (JsPath \ "title").read[String] and
    (JsPath \ "description").readNullable[String] and
    (JsPath \ "names").read[Seq[Name]] and
    (JsPath \ "features").read[Seq[Feature]] and
    (JsPath \ "reprPoint").readNullable[Coordinate] and
    (JsPath \ "place_types").read[Seq[String]]
  )(PleiadesRecord.apply _)
  
}


package models.place.crosswalks

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import models.{ HasGeometry, HasNullableSeq }
import models.place.{ Description, Gazetteer, GazetteerRecord, Name }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object PleiadesCrosswalk {
  
  def fromJson(record: String)(implicit sourceGazetteer: String): GazetteerRecord = {
    val result = Json.fromJson[PleiadesRecord](Json.parse(record))
    
    if (result.isError)
      play.api.Logger.warn(result.toString)
     
    val p = result.get
    GazetteerRecord(
      p.uri,
      Gazetteer(sourceGazetteer),
      DateTime.now(),
      None, // TODO lastChangedAt
      p.title,
      p.description.map(d => Seq(new Description(d))).getOrElse(Seq.empty[Description]),
      p.names,
      p.features.headOption.map(_.geometry), // TODO compute union?
      p.representativePoint,
      None, // TODO temporalBounds
      p.placeTypes,
      Seq.empty[String], // TODO closeMatches
      Seq.empty[String]  // TODO exactMatches
    )
  }
  
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

object PleiadesRecord extends HasGeometry with HasNullableSeq {
  
  implicit val pleiadesRecordReads: Reads[PleiadesRecord] = (
    (JsPath \ "uri").read[String] and
    (JsPath \ "title").read[String] and
    (JsPath \ "description").readNullable[String] and
    (JsPath \ "names").read[Seq[Name]] and
    (JsPath \ "features").read[Seq[Feature]] and
    (JsPath \ "reprPoint").readNullable[Coordinate] and
    (JsPath \ "place_types").readNullable[Seq[String]].map(_.getOrElse(Seq.empty[String]))
  )(PleiadesRecord.apply _)
  
}


package models.place.crosswalks

import com.vividsolutions.jts.geom.Coordinate
import models.{ HasGeometry, HasNullableSeq }
import models.place.{ CountryCode, Description, Gazetteer, GazetteerRecord, Name }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object GeoNamesCrosswalk extends BaseGeoJSONCrosswalk {
  
  private val GEONAMES = Gazetteer("GeoNames")
  
  def fromJson(record: String): Option[GazetteerRecord] = super.fromJson[GeoNamesRecord](record, { geonames =>
    GazetteerRecord(
      geonames.uri,
      GEONAMES,
      DateTime.now(),
      None, // lastChangedAt
      geonames.title,
      geonames.description.map(d => Seq(new Description(d))).getOrElse(Seq.empty[Description]),
      geonames.names,
      geonames.features.headOption.map(_.geometry), // TODO compute union?
      geonames.representativePoint,
      None, // temporalBounds
      Seq.empty[String], // place types
      geonames.countryCode.map(c => CountryCode(c.toUpperCase)),
      geonames.population,
      geonames.closeMatches,
      Seq.empty[String]  // exactMatches
    )
  })
  
}

case class GeoNamesRecord(

  uri: String,
  
  title: String,
  
  description: Option[String],
  
  names: Seq[Name],
  
  features: Seq[Feature],
  
  representativePoint: Option[Coordinate],
  
  countryCode: Option[String],
  
  population: Option[Long],
  
  closeMatches: Seq[String]
    
)

object GeoNamesRecord extends HasGeometry {
  
  implicit val geonamesRecordReads: Reads[GeoNamesRecord] = (
    (JsPath \ "uri").read[String] and
    (JsPath \ "title").read[String] and
    (JsPath \ "description").readNullable[String] and
    (JsPath \ "names").readNullable[Seq[Name]].map(_.getOrElse(Seq.empty[Name])) and
    (JsPath \ "features").readNullable[Seq[Feature]].map(_.getOrElse(Seq.empty[Feature])) and
    (JsPath \ "reprPoint").readNullable[Coordinate] and
    (JsPath \ "country_code").readNullable[String] and
    (JsPath \ "population").readNullable[Long] and
    (JsPath \ "close_matches").readNullable[Seq[String]].map(_.getOrElse(Seq.empty[String]))
  )(GeoNamesRecord.apply _)
  
}


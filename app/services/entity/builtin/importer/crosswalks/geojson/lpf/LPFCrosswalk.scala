package services.entity.builtin.importer.crosswalks.geojson.lpf

import com.vividsolutions.jts.geom.Geometry
import java.io.InputStream
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasGeometry
import services.entity._
import services.entity.builtin.importer.crosswalks.geojson.BaseGeoJSONCrosswalk

object LPFCrosswalk extends BaseGeoJSONCrosswalk {

  def fromGeoJSON(filename: String)(in: InputStream): Seq[EntityRecord] = {
    val source = filename.substring(0, filename.indexOf('.'))
    val maybeFc = Json.fromJson[LPFFeatureCollection](Json.parse(in)) // .get
    if (maybeFc.isError) println(maybeFc.toString)
    val fc = maybeFc.get
    fc.features.map(f => EntityRecord(
      f.id,
      source,
      DateTime.now().withZone(DateTimeZone.UTC),
      None, // lastChangedAt
      f.namings.headOption.map(_.toponym).getOrElse("[unnamed]"),
      Seq.empty[Description],
      Seq.empty[Name], // TODO create from namings
      f.geometry,
      f.geometry.map(_.getCentroid.getCoordinate),
      None, // country code
      None, // temporal bounds
      Seq.empty[String], // subjects
      None, // priority
      Seq.empty[Link] // TODO create from matches
    ))
  }
  
}

// To be extended in the future...
case class LPFFeature(
  id: String,
  namings: Seq[Naming],
  parthood: Seq[Parthood],
  placetypes: Seq[PlaceType],
  exactMatches: Seq[String],
  closeMatches: Seq[String],
  geometry: Option[Geometry])

object LPFFeature extends HasGeometry {
  
  implicit val lpfReads: Reads[LPFFeature] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "namings").readNullable[Seq[Naming]].map(_.getOrElse(Seq.empty[Naming])) and
    (JsPath \ "parthood").readNullable[Seq[Parthood]].map(_.getOrElse(Seq.empty[Parthood])) and
    (JsPath \ "placetypes").readNullable[Seq[PlaceType]].map(_.getOrElse(Seq.empty[PlaceType])) and
    (JsPath \ "exactMatch").readNullable[Seq[String]].map(_.getOrElse(Seq.empty[String])) and
    (JsPath \ "closeMatch").readNullable[Seq[String]].map(_.getOrElse(Seq.empty[String])) and
    (JsPath \ "geometry").readNullable[Geometry]
      .map[Option[Geometry]] {
        case Some(x) if x == null => None // Avoids Some(null) that happens for bad GeoJSON
        case other => other
      } 
  )(LPFFeature.apply _)
 
}

// TODO we really need to get rid of this redundancy...
case class LPFFeatureCollection(features: Seq[LPFFeature]) 
object LPFFeatureCollection {
  implicit val lpfFeatureCollectionReads: Reads[LPFFeatureCollection] =
    (JsPath \ "features").read[Seq[LPFFeature]].map(LPFFeatureCollection(_))
}

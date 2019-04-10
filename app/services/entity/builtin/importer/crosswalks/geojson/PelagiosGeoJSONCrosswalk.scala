package services.entity.builtin.importer.crosswalks.geojson

import com.vividsolutions.jts.geom.{Coordinate, Geometry}
import java.io.InputStream
import services.HasGeometry
import services.entity._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import play.api.libs.functional.syntax._

object PelagiosGeoJSONCrosswalk extends BaseGeoJSONCrosswalk {

  def fromGeoJSON(identifier: String)(in: InputStream): Seq[EntityRecord] = {
    val fc = Json.fromJson[PelagiosGazetteerFeatureCollection](Json.parse(in)).get
    fc.features.map(f => EntityRecord(
      f.uri,
      identifier,
      DateTime.now().withZone(DateTimeZone.UTC),
      None, // lastChangedAt
      f.title,
      f.descriptions,
      f.names,
      f.geometry,
      f.geometry.map(_.getCentroid.getCoordinate),
      None, // country code
      None, // temporal bounds
      Seq.empty[String], // subjects
      None, // priority
      Seq.empty[Link]
    ))
  }

}

case class PelagiosGazetteerFeature(
  uri                 : String,
  title               : String,
  descriptions        : Seq[Description],
  names               : Seq[Name],
  geometry            : Option[Geometry],
  representativePoint : Option[Coordinate])

case class PelagiosGazetteerFeatureCollection(features: Seq[PelagiosGazetteerFeature])

object PelagiosGazetteerFeature extends HasGeometry {

  implicit val pelagiosGeoJSONReads: Reads[PelagiosGazetteerFeature] = (
    (JsPath \ "uri").read[String] and
    (JsPath \ "title").read[String] and
    (JsPath \ "descriptions").readNullable[Seq[Description]].map(_.getOrElse(Seq.empty[Description])) and
    (JsPath \ "names").readNullable[Seq[Name]].map(_.getOrElse(Seq.empty[Name])) and
    (JsPath \ "geometry").readNullable[Geometry]
      .map[Option[Geometry]] {
        case Some(x) if x == null => None // Avoids Some(null) that happens for bad GeoJSON
        case other => other
      } and 
    (JsPath \ "representative_point").readNullable[Coordinate]
  )(PelagiosGazetteerFeature.apply _)
}

object PelagiosGazetteerFeatureCollection {

  implicit val pelagiosGazetteerFeatureCollectionRead: Reads[PelagiosGazetteerFeatureCollection] =
    (JsPath \ "features").read[Seq[PelagiosGazetteerFeature]].map(PelagiosGazetteerFeatureCollection(_))

}

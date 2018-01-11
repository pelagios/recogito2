package services.place.crosswalks

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import java.io.InputStream
import services.HasGeometry
import services.place.{ Description, Gazetteer, GazetteerRecord, Name }
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object PelagiosGeoJSONCrosswalk extends BaseGeoJSONCrosswalk {
  
  def fromGeoJSON(filename: String)(in: InputStream): Seq[GazetteerRecord] = {
    val source = filename.substring(0, filename.indexOf('.'))
    val fc = Json.fromJson[PelagiosGazetteerFeatureCollection](Json.parse(in)).get
    fc.features.map(f => GazetteerRecord(
      f.uri,
      Gazetteer(source),
      DateTime.now().withZone(DateTimeZone.UTC),
      None, // lastChangedAt
      f.title,
      f.descriptions,
      f.names,
      f.geometry,
      f.geometry.map(_.getCentroid.getCoordinate),
      None,
      Seq.empty[String], // place types
      None,
      None,
      Seq.empty[String], // closeMatches
      Seq.empty[String]  // exactMatches
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
    (JsPath \ "geometry").readNullable[Geometry] and
    (JsPath \ "representative_point").readNullable[Coordinate]
  )(PelagiosGazetteerFeature.apply _)
}

object PelagiosGazetteerFeatureCollection {

  implicit val pelagiosGazetteerFeatureCollectionRead: Reads[PelagiosGazetteerFeatureCollection] =
    (JsPath \ "features").read[Seq[PelagiosGazetteerFeature]].map(PelagiosGazetteerFeatureCollection(_))
  
}
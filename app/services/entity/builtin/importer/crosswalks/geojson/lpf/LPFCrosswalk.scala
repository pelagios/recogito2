package services.entity.builtin.importer.crosswalks.geojson.lpf

import com.vividsolutions.jts.geom.{Geometry, GeometryCollection}
import java.io.InputStream
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasGeometrySafe
import services.entity._
import services.entity.builtin.importer.crosswalks.geojson.BaseGeoJSONCrosswalk

object LPFCrosswalk extends BaseGeoJSONCrosswalk {
  
  private def toEntityRecord(source: String, f: LPFFeature) = EntityRecord(
    f.id,
    source,
    DateTime.now().withZone(DateTimeZone.UTC),
    None, // lastChangedAt
    f.title,
    f.descriptions.map(_.toDescription),
    f.namings.map(_.toName),
    f.normalizedGeometry,
    f.normalizedGeometry.map(_.getCentroid.getCoordinate),
    None, // country code
    None, // temporal bounds
    f.placetypes.map(_.label),
    None, // priority
    Seq.empty[Link] // TODO create from matches
  )

  def fromJsonLines(filename: String)(record: String): Option[EntityRecord] = super.fromJson[LPFFeature](record, { f =>
    val source = filename.substring(0, filename.indexOf('.'))
    toEntityRecord(source, f)
  })
  
  def fromGeoJSON(filename: String)(in: InputStream): Seq[EntityRecord] = {
    val source = filename.substring(0, filename.indexOf('.'))
    val maybeFc = Json.fromJson[LPFFeatureCollection](Json.parse(in)) // .get
    if (maybeFc.isError) println(maybeFc.toString)
    val fc = maybeFc.get
    fc.features.map(toEntityRecord(source, _))
  }
  
}

case class LPFFeature(
  id: String,
  title: String,
  countryCode: Option[String],
  namings: Seq[Naming],
  parthood: Seq[Parthood],
  placetypes: Seq[PlaceType],
  descriptions: Seq[LPFDescription],
  exactMatches: Seq[String],
  closeMatches: Seq[String],
  geometry: Option[Geometry]) {
  
  lazy val links = 
    closeMatches.map(Link(_, LinkType.CLOSE_MATCH))
    exactMatches.map(Link(_, LinkType.EXACT_MATCH))
    
  /** Simplifies single-geometry GeometryCollections to... single geometries **/
  lazy val normalizedGeometry = geometry.map { _ match {
    case geom: GeometryCollection => 
      if (geom.getNumGeometries == 1) geom.getGeometryN(0)
      else geom
    case geom => geom
  }}
  
}

object LPFFeature extends HasGeometrySafe {
  
  implicit val lpfReads: Reads[LPFFeature] = (
    (JsPath \ "@id").read[String] and
    (JsPath \ "properties"\ "title").read[String] and
    (JsPath \ "properties" \ "ccode").readNullable[String] and
    (JsPath \ "namings").readNullable[Seq[Naming]].map(_.getOrElse(Seq.empty[Naming])) and
    (JsPath \ "parthood").readNullable[Seq[Parthood]].map(_.getOrElse(Seq.empty[Parthood])) and
    (JsPath \ "placetypes").readNullable[Seq[PlaceType]].map(_.getOrElse(Seq.empty[PlaceType])) and
    (JsPath \ "description").readNullable[Seq[LPFDescription]].map(_.getOrElse(Seq.empty[LPFDescription])) and
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

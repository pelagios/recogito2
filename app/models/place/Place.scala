package models.place

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import java.io.StringWriter
import models.HasDate
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.geotools.geojson.geom.GeometryJSON

case class Place(
    
  /** The ID equals the URI of the gazetteer record added first **/
  id: String,
    
  /** URIs of all gazetteer records added to this place **/ 
  uris: Seq[String],
  
  /** One title - usually that of the first gazetteer record added **/
  title: String,
    
  /** Place types from all gazetteers (with source information) **/
  placeTypes: Seq[PlaceType],
    
  /** Descriptions from all gazetteers (with source information) **/
  descriptions: Seq[Literal],
    
  /** Names from all gazetteers (with source information) **/
  names: Seq[Literal],
    
  /** One representative geometry - usually that of a 'preferred geometry provider' **/ 
  representativeGeometry: Option[Geometry],
    
  /** One representative geometry **/
  representativePoint: Option[Coordinate],
  
  /** Union of the temporal bounds of all gazetteer records **/
  temporalBounds: Option[TemporalBounds],
  
  /** skos:closeMatch URIs from all gazetteers **/  
  closeMatches: Seq[String],
    
  /** skos:exactMatch URIs from all gazetteers **/
  exactMatches: Seq[String]
    
) {
  
  // For convenience
  val allMatches = closeMatches ++ exactMatches 
  
}

object Place {
  
  implicit val geometryWrites: Format[Geometry] =
    Format(
      JsPath.read[JsValue].map { json =>
        new GeometryJSON().read(Json.stringify(json))
      },
      
      Writes[Geometry] { geom =>
        val writer = new StringWriter()
        new GeometryJSON().write(geom, writer)
        Json.parse(writer.toString)
      }
    )
  
  implicit val coordinateWrites: Format[Coordinate] =
    Format(
      JsPath.read[JsArray].map { json =>
        val lon = json.value(0).as[Double]
        val lat = json.value(1).as[Double]
        new Coordinate(lon, lat)
      },
      
      Writes[Coordinate] { c =>
        Json.toJson(Seq(c.x, c.y))
      }
    )

  implicit val placeFormat: Format[Place] = (
    (JsPath \ "_id").format[String] and
    (JsPath \ "uris").format[Seq[String]] and
    (JsPath \ "title").format[String] and
    (JsPath \ "place_types").format[Seq[PlaceType]] and
    (JsPath \ "descriptions").format[Seq[Literal]] and
    (JsPath \ "names").format[Seq[Literal]] and
    (JsPath \ "representative_geometry").formatNullable[Geometry] and
    (JsPath \ "representative_point").formatNullable[Coordinate] and
    (JsPath \ "temporal_bounds").formatNullable[TemporalBounds] and
    (JsPath \ "close_matches").format[Seq[String]] and
    (JsPath \ "exact_matches").format[Seq[String]]
  )(Place.apply, unlift(Place.unapply))
  
}

case class Gazetteer(name: String)

object Gazetteer {
  
  implicit val gazetteerFormat: Format[Gazetteer] =
    Format(
      JsPath.read[String].map(Gazetteer(_)),
      Writes[Gazetteer](t => JsString(t.name))
    )
    
}

case class PlaceType(label: String, inGazetteer: Seq[Gazetteer])

object PlaceType {
  
  implicit val placeTypeFormat: Format[PlaceType] = (
    (JsPath \ "label").format[String] and
    (JsPath \ "in_gazetteer").format[Seq[Gazetteer]]
  )(PlaceType.apply, unlift(PlaceType.unapply))
    
}
    
case class Literal(label: String, language: Option[String], inGazetteer: Seq[Gazetteer])

object Literal {
  
  implicit val literalFormat: Format[Literal] = (
    (JsPath \ "label").format[String] and
    (JsPath \ "language").formatNullable[String] and
    (JsPath \ "in_gazetteer").format[Seq[Gazetteer]]
  )(Literal.apply, unlift(Literal.unapply))
  
}

case class TemporalBounds(from: DateTime, to: DateTime)

object TemporalBounds extends HasDate {
  
  implicit val temporalBoundsFormat: Format[TemporalBounds] = (
    (JsPath \ "from").format[DateTime] and
    (JsPath \ "language").format[DateTime]
  )(TemporalBounds.apply, unlift(TemporalBounds.unapply))
  
}

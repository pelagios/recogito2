package models.place

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import java.io.StringWriter
import models.HasDate
import org.geotools.geojson.geom.GeometryJSON
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Place(
    
  /** The ID equals the URI of the gazetteer record added first **/
  id: String,
    
  /** One title - usually that of the first gazetteer record added **/
  title: String,
    
  /** One representative geometry - usually that of a 'preferred geometry provider' **/ 
  geometry: Option[Geometry],
    
  /** One representative geometry **/
  representativePoint: Option[Coordinate],
  
  /** Union of the temporal bounds of all gazetteer records **/
  temporalBounds: Option[TemporalBounds],
  
  /** The gazetteer records that define that place **/
  isConflationOf: Seq[GazetteerRecord]
  
)

/** JSON (de)serialization **/

object Place {
  
  implicit val geometryFormat: Format[Geometry] =
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
  
  implicit val coordinateFormat: Format[Coordinate] =
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
    (JsPath \ "title").format[String] and
    (JsPath \ "geometry").formatNullable[Geometry] and
    (JsPath \ "representative_point").formatNullable[Coordinate] and
    (JsPath \ "temporal_bounds").formatNullable[TemporalBounds] and
    (JsPath \ "is_conflation_of").format[Seq[GazetteerRecord]]
  )(Place.apply, unlift(Place.unapply))
  
}
package services

import com.vividsolutions.jts.geom.{Coordinate, Envelope, Geometry}
import java.io.StringWriter
import org.geotools.geojson.geom.GeometryJSON
import play.api.libs.json._

trait HasGeometry {

  private val DECIMAL_PRECISION = 12
  
  implicit val geometryFormat: Format[Geometry] =
    Format(
      JsPath.read[JsValue].map { json =>
        val geom = new GeometryJSON(DECIMAL_PRECISION).read(Json.stringify(json))
        if (geom.isEmpty) null else geom // Some Java legacy...
      },
      
      Writes[Geometry] { geom =>
        val writer = new StringWriter()
        new GeometryJSON(DECIMAL_PRECISION).write(geom, writer)
        Json.parse(writer.toString)
      }
    )
    
  implicit val envelopeFormat: Format[Envelope] = 
    Format(
      (JsPath \ "coordinates").read[JsArray].map { js =>
        val points = js.value.map(_.as[JsArray].value.map(_.as[Double]))
        val topLeft = points(0)
        val bottomRight = points(1)
        new Envelope(topLeft(0), bottomRight(0), bottomRight(1), topLeft(1))
      },
        
      Writes[Envelope] { e =>
        Json.obj(
          "type" -> "envelope", // A special geo_shape type supported by ElasticSearch
          "coordinates" -> Seq(Seq(e.getMinX, e.getMaxY), Seq(e.getMaxX, e.getMinY))) 
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

}

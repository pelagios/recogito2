package models

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import java.io.StringWriter
import org.geotools.geojson.geom.GeometryJSON
import play.api.libs.json._

trait HasGeometry {

  private val DECIMAL_PRECISION = 12
  
  implicit val geometryFormat: Format[Geometry] =
    Format(
      JsPath.read[JsValue].map { json =>
        new GeometryJSON(DECIMAL_PRECISION).read(Json.stringify(json))
      },
      
      Writes[Geometry] { geom =>
        val writer = new StringWriter()
        new GeometryJSON(DECIMAL_PRECISION).write(geom, writer)
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

}

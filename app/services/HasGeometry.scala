package services

import com.vividsolutions.jts.geom.{Coordinate, Envelope, Geometry}
import java.io.StringWriter
import org.geotools.geojson.geom.GeometryJSON
import play.api.libs.json._

trait HasGeometry {

  protected val DECIMAL_PRECISION = 12

  implicit val geometryFormat: Format[Geometry] =
    Format(
      JsPath.read[JsValue].map { json =>
        try {
          val geom = new GeometryJSON(DECIMAL_PRECISION).read(Json.stringify(json))
          if (geom != null && geom.isEmpty) null else geom // Some Java legacy...
        } catch { case t: Throwable =>
          null
        }
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

trait HasGeometrySafe extends HasGeometry {

  /** Utility method to perform data normalization on the geometry object.
    *
    * It looks as if the GeoTools parser (as well as other Java parsers)
    * break when they encounter a geometry object that has more than the
    * mandatory 'type' and 'coordinates' fields. (At least I could reproducibly
    * break parsing with a sample that had a 'properties' field with nested
    * objects.)
    *
    * This method filters the geometry object, so that only 'type' and 'coordinates'
    * remain.
    */
  private def safeGeometry(json: JsValue): JsObject =
    JsObject(
      Json.obj(
        "type" -> (json \ "type").get,
        "coordinates" -> (json \ "coordinates").asOpt[JsArray],
        "geometries" -> (json \ "geometries").asOpt[JsArray].map(_.value.map(safeGeometry))
      ).fields.filter(_._2 match { // Filter out undefined props
        case JsNull => false
        case _ => true
      })
    )

  implicit override val geometryFormat: Format[Geometry] =
    Format(
      JsPath.read[JsValue].map { json =>
        new GeometryJSON(DECIMAL_PRECISION).read(Json.stringify(safeGeometry(json)))
      },

      Writes[Geometry] { geom =>
        val writer = new StringWriter()
        new GeometryJSON(DECIMAL_PRECISION).write(geom, writer)
        Json.parse(writer.toString)
      }
    )

}

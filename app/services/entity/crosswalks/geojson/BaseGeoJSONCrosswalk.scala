package services.entity.crosswalks.geojson

import com.vividsolutions.jts.geom.Geometry
import services.HasGeometry
import services.entity.EntityRecord
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._

trait BaseGeoJSONCrosswalk {

  def fromJson[T](record: String, crosswalk: T => EntityRecord)(implicit reads: Reads[T]): Option[EntityRecord] =
    Json.fromJson[T](Json.parse(record)) match {
      case s: JsSuccess[T] => Some(crosswalk(s.get))
      case e: JsError =>
        Logger.warn(e.toString)
        None
    }

}

case class Feature(geometry: Geometry)

object Feature extends HasGeometry {

  implicit val featureReads: Reads[Feature] = (JsPath \ "geometry").read[Geometry].map(Feature(_))

}

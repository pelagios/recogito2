package models.place.crosswalks

import com.vividsolutions.jts.geom.Geometry
import models.HasGeometry
import models.place.GazetteerRecord
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

trait BaseGeoJSONCrosswalk {
  
  def fromJson[T](record: String, crosswalk: T => GazetteerRecord)(implicit reads: Reads[T]): Option[GazetteerRecord] =
    
    Json.fromJson[T](Json.parse(record)) match {
    
      case s: JsSuccess[T] => Some(crosswalk(s.get))
        
      case e: JsError =>
        Logger.error(e.toString)      
        None
        
    }
  
}

case class Feature(geometry: Geometry)

object Feature extends HasGeometry {
  
  implicit val featureReads: Reads[Feature] =
    (JsPath \ "geometry").read[Geometry].map(Feature(_))
  
}
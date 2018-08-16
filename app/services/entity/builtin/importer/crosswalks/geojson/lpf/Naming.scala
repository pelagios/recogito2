package services.entity.builtin.importer.crosswalks.geojson.lpf

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Naming(toponym: String)

object Naming {
  
  implicit val namingReads = (JsPath \ "toponym").read[JsString].map { json =>
    Naming(json.value)
  }
 
}
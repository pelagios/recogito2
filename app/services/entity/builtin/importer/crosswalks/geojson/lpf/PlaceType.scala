package services.entity.builtin.importer.crosswalks.geojson.lpf

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class PlaceType(id: String, label: String)

object PlaceType {
  
  implicit val placetypeReads: Reads[PlaceType] = (
    (JsPath \ "@id").read[JsValue].map(_.toString) and // Could be numbers...
    (JsPath \ "label").read[String]
  )(PlaceType.apply _)
  
}

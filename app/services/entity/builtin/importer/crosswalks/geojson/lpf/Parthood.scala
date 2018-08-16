package services.entity.builtin.importer.crosswalks.geojson.lpf

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Parthood(parent: String, label: String)

object Parthood {
  
  implicit val parthoodReads: Reads[Parthood] = (
    (JsPath \ "parent").read[String] and
    (JsPath \ "label").read[String]
  )(Parthood.apply _)
  
}
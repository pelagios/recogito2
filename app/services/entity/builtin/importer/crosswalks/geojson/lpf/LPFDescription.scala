package services.entity.builtin.importer.crosswalks.geojson.lpf

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.entity.Description

case class LPFDescription(value: String, lang: Option[String], source: Option[String]) {
  
  lazy val toDescription = Description(value, lang)
    
}

object LPFDescription {
  
  implicit val lpfDescriptionReads: Reads[LPFDescription] = (
    (JsPath \ "value").read[String] and
    (JsPath \ "lang").readNullable[String] and
    (JsPath \ "source").readNullable[String]
  )(LPFDescription.apply _)
  
}
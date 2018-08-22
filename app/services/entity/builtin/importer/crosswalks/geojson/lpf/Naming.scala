package services.entity.builtin.importer.crosswalks.geojson.lpf

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.entity.Name

case class Naming(toponym: String, lang: Option[String]) {
  
  private lazy val normalizedLang = lang match {
    case Some(l) if !l.trim.isEmpty => Some(l.trim)
    case _ => None
  }
  
  lazy val toName = Name(toponym, lang)
  
}

object Naming {
  
  implicit val namingReads: Reads[Naming] = (
    (JsPath \ "toponym").read[String] and
    (JsPath \ "lang").readNullable[String]
  )(Naming.apply _)
 
}
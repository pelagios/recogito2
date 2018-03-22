package transform.iiif.api

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class PlainLiteral(value: String, language: Option[String] = None)

object PlainLiteral {
  
  implicit val plainLiteralReads =
    (JsPath).read[JsValue] map {
      case obj: JsObject =>
        val value = (obj \ "@value").as[String]
        val lang =  (obj \ "@language").asOpt[String]
        PlainLiteral(value, lang)
        
      case str: JsString => PlainLiteral(str.value)
        
      case json => throw new RuntimeException(s"Could not parse literal: ${json}")    
    }

}
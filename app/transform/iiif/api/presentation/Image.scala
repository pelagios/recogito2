package transform.iiif.api.presentation

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Image(width: Int, height: Int, service: String)

object Image {
  
  implicit val imageReads: Reads[Image] = (
    (JsPath \ "resource" \ "width").read[Int] and
    (JsPath \ "resource" \ "height").read[Int] and
    (JsPath \ "resource" \ "service" \ "@id").read[String]
  )(Image.apply _)
  
}

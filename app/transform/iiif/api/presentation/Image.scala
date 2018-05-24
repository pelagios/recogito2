package transform.iiif.api.presentation

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Image(width: Int, height: Int, service: String)

object Image {
  
  implicit val imageReads: Reads[Image] = (
    (JsPath \ "resource" \ "width").read[Int] and
    (JsPath \ "resource" \ "height").read[Int] and
    (JsPath \ "resource" \ "service" \ "@id").read[String]
      .map { url =>
        // Some manifests omit the info.json suffix, but then 
        // require it for proper URL resolution (sigh)
        if (url.endsWith("info.json")) url
        else if (url.endsWith("/")) s"${url}info.json"
        else s"${url}/info.json"
      }
  )(Image.apply _)
  
}

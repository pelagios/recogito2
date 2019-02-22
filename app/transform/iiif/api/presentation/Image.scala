package transform.iiif.api.presentation

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Image(width: Int, height: Int, service: String)

object Image {
  
  implicit val imageReads: Reads[Image] = (
    (JsPath \ "resource" \ "width").readNullable[Int] and
    (JsPath \ "resource" \ "height").readNullable[Int] and
    (JsPath \ "resource" \ "service" \ "width").readNullable[Int] and
    (JsPath \ "resource" \ "service" \ "height").readNullable[Int] and
    (JsPath \ "resource" \ "service" \ "@id").read[String]
      .map { url =>
        // Some manifests omit the info.json suffix, but then 
        // require it for proper URL resolution (sigh)
        if (url.endsWith("info.json")) url
        else if (url.endsWith("/")) s"${url}info.json"
        else s"${url}/info.json"
      }
  ).tupled.map(t => Image.apply(t._1, t._2, t._3, t._4, t._5))

  def apply(
    resourceWidth: Option[Int], 
    resourceHeight: Option[Int],
    serviceWidth: Option[Int],
    serviceHeight: Option[Int],
    service: String
  ) = {
    // Pick the first available, but fail hard if none at all
    val width = Seq(resourceWidth, serviceWidth).flatten.head
    val height = Seq(resourceHeight, serviceHeight).flatten.head
    new Image(width, height, service)
  }
  
}

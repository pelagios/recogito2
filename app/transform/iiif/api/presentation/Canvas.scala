package transform.iiif.api.presentation

import play.api.libs.json._
import play.api.libs.functional.syntax._
import transform.iiif.api.PlainLiteral

case class Canvas(label: PlainLiteral, images: Seq[Image])

object Canvas {
  
  implicit val canvasReads: Reads[Canvas] = (
    (JsPath \ "label").read[PlainLiteral] and
    (JsPath \ "images").read[Seq[Image]]
  )(Canvas.apply _)
  
}

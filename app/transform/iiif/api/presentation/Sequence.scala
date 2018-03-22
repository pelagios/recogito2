package transform.iiif.api.presentation

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Sequence(canvases: Seq[Canvas])

object Sequence {
  
  implicit val sequenceReads = 
    (JsPath \ "canvases").read[Seq[Canvas]].map(Sequence(_))
  
}

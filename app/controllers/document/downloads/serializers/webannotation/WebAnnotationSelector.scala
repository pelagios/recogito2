package controllers.document.downloads.serializers.webannotation

import play.api.libs.json._
import play.api.libs.functional.syntax._

sealed trait WebAnnotationSelector

case class TextPositionSelector(start: Int, end: Int)

object TextPositionSelector {
  
  implicit val textPositionSelectorWrites: Writes[TextPositionSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "start").write[Int] and
    (JsPath \ "end").write[Int]
  )(s => ("TextPositionSelector", s.start, s.end))
  
}


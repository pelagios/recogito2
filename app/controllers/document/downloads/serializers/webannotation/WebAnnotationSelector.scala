package controllers.document.downloads.serializers.webannotation

import models.annotation.{ Annotation, AnnotationBody }
import play.api.libs.json._
import play.api.libs.functional.syntax._

sealed trait WebAnnotationSelector

object WebAnnotationSelector {
  
  implicit val webAnnotationSelectorWriter = Writes[WebAnnotationSelector] {
    case s: TextPositionSelector => Json.toJson(s)
    case s: TextQuoteSelector    => Json.toJson(s)
    case s: FragmentSelector     => Json.toJson(s)
  }
  
}

case class TextPositionSelector(start: Int, end: Int) extends WebAnnotationSelector

object TextPositionSelector {
  
  def fromAnnotation(a: Annotation) = {
    if (a.anchor.startsWith("char-offset:")) {
      val start = a.anchor.substring(12).toInt
      val len = a.bodies.find(_.hasType == AnnotationBody.QUOTE).flatMap { b =>
        b.value.map(_.size)
      }.getOrElse(-1) // Should never happen
      
      TextPositionSelector(start, start + len)
    } else {
      throw new IllegalArgumentException("Cannot build TextPositionSelector from non-text annotation")
    }
  }
  
  implicit val textPositionSelectorWrites: Writes[TextPositionSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "start").write[Int] and
    (JsPath \ "end").write[Int]
  )(s => ("TextPositionSelector", s.start, s.end))
  
}

case class TextQuoteSelector(quote: String) extends WebAnnotationSelector

object TextQuoteSelector {
  
  implicit val textQuoteSelectorWrites: Writes[TextQuoteSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "quote").write[String]
  )(s => ("TextQuoteSelector", s.quote))
  
}

case class FragmentSelector(value: String) extends WebAnnotationSelector

object FragmentSelector {
  
  implicit val fragmentSelectorWrites: Writes[FragmentSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "conformsTo").write[String] and
    (JsPath \ "value").write[String]
  )(s => ("FragmentSelector", "http://www.w3.org/TR/media-frags/", s.value)) 
  
}


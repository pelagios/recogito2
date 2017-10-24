package controllers.document.downloads.serializers.webannotation

import models.image._
import models.annotation.{ Annotation, AnnotationBody }
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** 
  *  Base marker trait for all selectors we implement 
  */
sealed trait WebAnnotationSelector

object WebAnnotationSelector {

  // If someone knows a way to improve this, let me know...
  implicit val webAnnotationSelectorWriter = Writes[WebAnnotationSelector] {
    case s: TextPositionSelector => Json.toJson(s)
    case s: TextQuoteSelector    => Json.toJson(s)
    case s: FragmentSelector     => Json.toJson(s)
  }
  
}

/** 
  *  https://www.w3.org/TR/annotation-model/#text-position-selector
  */
case class TextPositionSelector(start: Int, end: Int) extends WebAnnotationSelector

object TextPositionSelector {
  
  def fromAnnotation(a: Annotation) = {
    val quoteBody = a.bodies.find(_.hasType == AnnotationBody.QUOTE)
    val isPlainText = a.anchor.startsWith("char-offset:")
    
    if (quoteBody.isDefined && isPlainText) {
      val start = a.anchor.substring(12).toInt
      val len = quoteBody.get.value.map(_.size).getOrElse(-1) // Should never happen
      TextPositionSelector(start, start + len)
    } else {
      throw new IllegalArgumentException(s"Unable to build TextPositionSelector for annotation ${a}")
    }
  }
  
  implicit val textPositionSelectorWrites: Writes[TextPositionSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "start").write[Int] and
    (JsPath \ "end").write[Int]
  )(s => ("TextPositionSelector", s.start, s.end))
  
}

/** 
  * https://www.w3.org/TR/annotation-model/#text-quote-selector
  *
  * TODO implement 'prefix' and 'suffix' fields
  */
case class TextQuoteSelector(quote: String) extends WebAnnotationSelector

object TextQuoteSelector {
  
  def fromAnnotation(a: Annotation) = {
    val quoteBody = a.bodies.find(_.hasType == AnnotationBody.QUOTE)
    
    // TODO support TEI content
    val isText = a.anchor.startsWith("char-offset:")
    
    if (quoteBody.isDefined && isText)
      TextQuoteSelector(quoteBody.get.value.getOrElse(""))
    else
      throw new IllegalArgumentException(s"Unable to build TextQuoteSelector for annotation ${a}")
  }
  
  implicit val textQuoteSelectorWrites: Writes[TextQuoteSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "exact").write[String]
  )(s => ("TextQuoteSelector", s.quote))
  
}

case class FragmentSelector(value: String) extends WebAnnotationSelector

object FragmentSelector {
  
  def fromAnnotation(a: Annotation): FragmentSelector = ImageAnchor.parse(a.anchor) match {
   case a: PointAnchor =>
     FragmentSelector(s"xywh=pixel:${a.x},${a.y},0,0")
     
   case a: RectAnchor =>
     FragmentSelector(s"xywh=pixel:${a.x},${a.y},${a.w},${a.h}")
   
   case a: TiltedBoxAnchor =>
     val b = a.bounds
     FragmentSelector(s"xywh=pixel:${b.left},${b.top},${b.width},${b.height}")
  }
  
  implicit val fragmentSelectorWrites: Writes[FragmentSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "conformsTo").write[String] and
    (JsPath \ "value").write[String]
  )(s => ("FragmentSelector", "http://www.w3.org/TR/media-frags/", s.value)) 
  
}


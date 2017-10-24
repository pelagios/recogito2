package controllers.document.downloads.serializers.webannotation

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
  
  private def toMap(str: String) =
    str.split(",").map { pair =>
      val key = pair.substring(0, pair.indexOf('='))
      val value = pair.substring(pair.indexOf('=') + 1)
      (key -> value)
    }.toMap
  
  def fromAnnotation(a: Annotation): FragmentSelector = a.anchor match {
    
    case a if a.startsWith("point") =>
      // Example: point:839,590
      val x = a.substring(6, a.indexOf(','))
      val y = a.substring(a.indexOf(',') + 1)
      FragmentSelector(s"xywh=${x},${y},0,0")
      
    case a if a.startsWith("rect") =>
      // Example: rect:x=441,y=399,w=88,h=106
      val args = toMap(a.substring(5))
      val x = args.get("x").get
      val y = args.get("y").get
      val w = args.get("w").get
      val h = args.get("h").get
      FragmentSelector(s"xywh=pixel:${x},${y},${w},${h}")
      
    case a if a.startsWith("tbox") =>
      // Example: tbox:x=927,y=575,a=0,l=64,h=22
      val args = toMap(a.substring(5))
      // TODO
      FragmentSelector("undefined")
  }
  
  implicit val fragmentSelectorWrites: Writes[FragmentSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "conformsTo").write[String] and
    (JsPath \ "value").write[String]
  )(s => ("FragmentSelector", "http://www.w3.org/TR/media-frags/", s.value)) 
  
}


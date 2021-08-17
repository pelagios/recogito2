package controllers.document.downloads.serializers.annotations.webannotation

import services.image._
import services.annotation.{Annotation, AnnotationBody}
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import com.vividsolutions.jts.geom.{Coordinate, Point, PrecisionModel, GeometryFactory}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** 
  *  Base marker trait for all selectors we implement 
  */
sealed trait WebAnnotationSelector

object WebAnnotationSelector {

  // If someone knows a way to improve this, let me know...
  implicit val webAnnotationSelectorWriter = Writes[WebAnnotationSelector] {
    case s: TextPositionSelector  => Json.toJson(s)
    case s: TextQuoteSelector     => Json.toJson(s)
    case s: ImageFragmentSelector => Json.toJson(s)
    case s: MapFragmentSelector   => Json.toJson(s)
    case s: TableFragmentSelector => Json.toJson(s)
    case s: XPathRangeSelector    => Json.toJson(s)
    case s: SvgSelector           => Json.toJson(s)
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
  
  def fromAnnotation(a: Annotation) =
    a.bodies.find(_.hasType == AnnotationBody.QUOTE) match {
      case Some(quoteBody) =>
        TextQuoteSelector(quoteBody.value.getOrElse(""))
      case _ =>
      throw new IllegalArgumentException(s"Unable to build TextQuoteSelector for annotation ${a}")
    }
  
  implicit val textQuoteSelectorWrites: Writes[TextQuoteSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "exact").write[String]
  )(s => ("TextQuoteSelector", s.quote))
  
}

/**
  * https://www.w3.org/TR/annotation-model/#fragment-selector
  * 
  * TODO should we add SVG selector serialization?
  */
case class ImageFragmentSelector(value: String) extends WebAnnotationSelector

object ImageFragmentSelector {
  
  def fromAnnotation(a: Annotation) = ImageAnchor.parse(a.anchor) match {
    case a: PointAnchor =>
      ImageFragmentSelector(s"xywh=pixel:${a.x},${a.y},0,0")
     
    case a: RectAnchor =>
      ImageFragmentSelector(s"xywh=pixel:${a.x},${a.y},${a.w},${a.h}")
   
    case a: TiltedBoxAnchor =>
      // Tilted boxes are 'dumbed down' to their bounds
      val b = a.bounds
      ImageFragmentSelector(s"xywh=pixel:${b.left},${b.top},${b.width},${b.height}")

    case a: SvgAnchor =>
      SvgSelector(a.svg)
   
  }
  
  implicit val imageFragmentSelectorWrites: Writes[ImageFragmentSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "conformsTo").write[String] and
    (JsPath \ "value").write[String]
  )(s => ("FragmentSelector", "http://www.w3.org/TR/media-frags/", s.value)) 
  
}

case class SvgSelector(value: String) extends WebAnnotationSelector

object SvgSelector {

  implicit val svgSelectorWrites: Writes[SvgSelector] = (
    (JsPath \ "type").write[String] and 
    (JsPath \ "value").write[String]
  )(s => ("SvgSelector", s.value))

}

/**
 * TODO this is just a repurposing of ImageFragmentSelector. Later down the line,
 * we'll want to give more specific treatment to map fragments
 */
case class MapFragmentSelector(value: String) extends WebAnnotationSelector

object MapFragmentSelector {

  private def convert(x: Double, y: Double): Point = {
    val sourceCRS = CRS.decode("EPSG:3857")
    val targetCRS = CRS.decode("EPSG:4326")
    val transform = CRS.findMathTransform(sourceCRS, targetCRS, false)

    val factory = new GeometryFactory(new PrecisionModel(), 4326)

    val pt = factory.createPoint(new Coordinate(x, y))

    return JTS.transform(pt, transform).asInstanceOf[Point]
  }

  def fromAnnotation(a: Annotation) = ImageAnchor.parse(a.anchor) match {
    case a: PointAnchor =>
      val pt = convert(a.x, a.y)
      MapFragmentSelector(s"xywh=pixel:${pt.getY},${-pt.getX},0,0")

    case a: RectAnchor =>
      ImageFragmentSelector(s"xywh=pixel:${a.x},${a.y},${a.w},${a.h}")
   
    case a: TiltedBoxAnchor =>
      // Tilted boxes are 'dumbed down' to their bounds
      val b = a.bounds
      ImageFragmentSelector(s"xywh=pixel:${b.left},${b.top},${b.width},${b.height}")

    case a: SvgAnchor =>
      SvgSelector(a.svg)
    
  }

  implicit val mapFragmentSelectorWrites: Writes[MapFragmentSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "conformsTo").write[String] and
    (JsPath \ "value").write[String]
  )(s => ("FragmentSelector", "http://www.w3.org/TR/media-frags/", s.value)) 

}

case class TableFragmentSelector(row: Int) extends WebAnnotationSelector

object TableFragmentSelector {
  
  def fromAnnotation(a: Annotation) =
    if (a.anchor.startsWith("row")) {
     val row = a.anchor.substring(4).toInt
      TableFragmentSelector(row)      
    } else {
      throw new IllegalArgumentException(s"Unable to build CSV FragmentSelector for annotation ${a}")
    }
  
  implicit val tableFragmentSelectorWrites: Writes[TableFragmentSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "conformsTo").write[String] and
    (JsPath \ "value").write[String]
  )(s => ("FragmentSelector", "https://tools.ietf.org/html/rfc7111", "row=" + s.row)) 
  
}

/**
  * https://www.w3.org/TR/annotation-model/#xpath-selector 
  */
case class XPathSelector(value: String)

object XPathSelector {
  
  implicit val xpathSelectorWrites: Writes[XPathSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "value").write[String]
  )(s => ("XPathSelector", s.value))  
  
}

case class XPathRangeSelector(start: XPathSelector, end: XPathSelector) extends WebAnnotationSelector

object XPathRangeSelector {
  
  def fromAnnotation(a: Annotation): XPathRangeSelector = {
    // Example: from=/tei/text/body/div[6]/p::58;to=/tei/text/body/div[6]/p::67
    val pair = a.anchor.split(";")
    val from = pair(0).substring(5, pair(0).indexOf("::"))
    val to = pair(1).substring(3, pair(1).indexOf("::")) 
    XPathRangeSelector(XPathSelector(from), XPathSelector(to))
  }
  
  implicit val xpathRangeSelectorWrites: Writes[XPathRangeSelector] = (
    (JsPath \ "type").write[String] and
    (JsPath \ "startSelector").write[XPathSelector] and
    (JsPath \ "endSelector").write[XPathSelector]
  )(s => ("RangeSelector", s.start, s.end))  
  
}

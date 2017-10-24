package controllers.document.downloads.serializers.webannotation

import models.annotation.Annotation
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{ AnyContent, Request }

case class WebAnnotationTarget(source: String, hasType: String, selectors: Seq[WebAnnotationSelector])

object WebAnnotationTarget {
  
  def fromAnnotation(annotation: Annotation)(implicit req: Request[AnyContent]) = {
    val docId = annotation.annotates.documentId
    val partId = annotation.annotates.filepartId
    val target = controllers.document.annotation.routes.AnnotationController.resolveAnnotationView(docId, Some(partId), None).absoluteURL
    
    import models.ContentType._
    
    annotation.annotates.contentType match {
      // Plaintest gets a Text target with two alternative selectors: TextQuote + TextPosition
      case TEXT_PLAIN => 
        WebAnnotationTarget(target, "Text", Seq(
          TextPositionSelector.fromAnnotation(annotation),
          TextQuoteSelector.fromAnnotation(annotation)))
        
      // TODO TEI gets a Text target with two alternative selectors: TextQuote + XPath Range
      case TEXT_TEIXML =>
        WebAnnotationTarget(target, "Text", Seq(
          TextQuoteSelector.fromAnnotation(annotation),
          XPathRangeSelector.fromAnnotation(annotation)))
       
      // Images get an Image target with a fragment selector
      case IMAGE_UPLOAD | IMAGE_IIIF => 
        WebAnnotationTarget(target, "Image", Seq(ImageFragmentSelector.fromAnnotation(annotation)))
        
      // CSV gets a Dataset target with a Data Position selector
      case DATA_CSV  =>
        WebAnnotationTarget(target, "Dataset", Seq(TableFragmentSelector.fromAnnotation(annotation)))
        
      case c =>
        // Should never happen
        throw new IllegalArgumentException(s"Unable to create Web Annotation target for content type ${c}")
    }

  }
  
  implicit val webAnnotationTargetWrites: Writes[WebAnnotationTarget] = (
    (JsPath \ "source").write[String] and
    (JsPath \ "type").write[String] and
    (JsPath \ "selector").write[Seq[WebAnnotationSelector]]
  )(t => (t.source, t.hasType, t.selectors))

}


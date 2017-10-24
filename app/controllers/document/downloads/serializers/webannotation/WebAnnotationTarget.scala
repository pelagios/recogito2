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
        
      // TODO TEI gets a Text target with two alternative selectors: TextQuote + XPath
      case TEXT_TEIXML =>
        WebAnnotationTarget(target, "Text", Seq.empty[WebAnnotationSelector])
       
      // TODO Images get an Image target with a fragment selector
      // TODO we could diversify later, to add selectors based on anchor type
      case IMAGE_UPLOAD | IMAGE_IIIF => 
        WebAnnotationTarget(target, "Image", Seq.empty[WebAnnotationSelector])
        
      // TODO CSV gets a Dataset target with a Data Position selector
      case DATA_CSV  =>
        WebAnnotationTarget(target, "Dataset", Seq.empty[WebAnnotationSelector])
    }

  }
  
  implicit val webAnnotationTargetWrites: Writes[WebAnnotationTarget] = (
    (JsPath \ "source").write[String] and
    (JsPath \ "type").write[String] and
    (JsPath \ "selector").write[Seq[WebAnnotationSelector]]
  )(t => (t.source, t.hasType, t.selectors))

}


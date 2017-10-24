package controllers.document.downloads.serializers.webannotation

import models.annotation.Annotation
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{ AnyContent, Request }

case class WebAnnotationTarget(source: String, hasType: String, selector: WebAnnotationSelector)

object WebAnnotationTarget {
  
  def fromAnnotation(annotation: Annotation)(implicit req: Request[AnyContent]) = {
    val docId = annotation.annotates.documentId
    val partId = annotation.annotates.filepartId
    val target = controllers.document.annotation.routes.AnnotationController.resolveAnnotationView(docId, Some(partId), None).absoluteURL
    
    import models.ContentType._
    
    val targetType = annotation.annotates.contentType match {
      case t if t.isText  => "Text"
      case t if t.isImage => "Image"
      case t if t.isData  => "Dataset"
    }

    WebAnnotationTarget(target, targetType, TextPositionSelector.fromAnnotation(annotation))    
  }
  
  implicit val webAnnotationTargetWrites: Writes[WebAnnotationTarget] = (
    (JsPath \ "source").write[String] and
    (JsPath \ "type").write[String] and
    (JsPath \ "selector").write[WebAnnotationSelector]
  )(t => (t.source, t.hasType, t.selector))

}


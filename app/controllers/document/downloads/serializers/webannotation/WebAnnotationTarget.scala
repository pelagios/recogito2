package controllers.document.downloads.serializers.webannotation

import models.annotation.Annotation
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class WebAnnotationTarget(source: String, hasType: String, selector: WebAnnotationSelector)

object WebAnnotationTarget {
  
  def fromAnnotation(annotation: Annotation) = {
    
    // TODO dummy hack for testing only
    WebAnnotationTarget("", "", TextPositionSelector.fromAnnotation(annotation))
    
  }
  
  implicit val webAnnotationTargetWrites: Writes[WebAnnotationTarget] = (
    (JsPath \ "source").write[String] and
    (JsPath \ "type").write[String] and
    (JsPath \ "selector").write[WebAnnotationSelector]
  )(t => (t.source, t.hasType, t.selector))

}


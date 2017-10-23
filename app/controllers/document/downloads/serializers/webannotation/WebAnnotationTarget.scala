package controllers.document.downloads.serializers.webannotation

import models.annotation.Annotation
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class WebAnnotationTarget[T <: WebAnnotationSelector](source: String, hasType: String, selector: T)

object WebAnnotationTarget {
  
  def fromAnnotation[T <: WebAnnotationSelector](annotation: Annotation): WebAnnotationTarget[T] = ???
  
  implicit def webAnnotationTargetWrites[T <: WebAnnotationSelector](implicit w: Writes[T]): Writes[WebAnnotationTarget[T]] = (
    (JsPath \ "source").write[String] and
    (JsPath \ "type").write[String] and
    (JsPath \ "selector").write[T]
  )(t => (t.source, t.hasType, t.selector))

}


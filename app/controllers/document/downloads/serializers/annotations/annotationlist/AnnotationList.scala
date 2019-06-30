package controllers.document.downloads.serializers.annotations.annotationlist

import play.api.mvc.{AnyContent, Request}
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class AnnotationList(id: String, resources: Seq[AnnotationResource])

object AnnotationList {
    
  implicit def annotationListWrites(implicit request: Request[AnyContent]): Writes[AnnotationList] = (
    (JsPath \ "@context").write[String] and
    (JsPath \ "@id").write[String] and
    (JsPath \ "@type").write[String] and
    (JsPath \ "resources").write[Seq[AnnotationResource]]
  )(a => (
    "http://iiif.io/api/presentation/2/context.json",
    a.id,
    "sc:AnnotationList",
    a.resources
  ))

}
package controllers.document.downloads.serializers.webannotation

import models.HasDate
import models.annotation.Annotation
import org.joda.time.DateTime
import play.api.mvc.{ AnyContent, Request }
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class WebAnnotation(recogitoBaseURI: String, documentURI: String, annotation: Annotation)

object WebAnnotation extends HasDate {
  
  implicit def webAnnotationWrites(implicit request: Request[AnyContent]): Writes[WebAnnotation] = (
    (JsPath \ "@context").write[String] and
    (JsPath \ "id").write[String] and
    (JsPath \ "type").write[String] and
    (JsPath \ "generator").write[String] and
    (JsPath \ "generated").write[DateTime] and
    (JsPath \ "body").write[Seq[WebAnnotationBody]] and
    (JsPath \ "target").write[WebAnnotationTarget]
  )(a => (
    "http://www.w3.org/ns/anno.jsonld",
    a.documentURI + "#" + a.annotation.annotationId.toString,
    "Annotation",
    a.recogitoBaseURI,
    DateTime.now,
    a.annotation.bodies.flatMap(b => WebAnnotationBody.fromAnnotationBody(b, a.recogitoBaseURI)),
    WebAnnotationTarget.fromAnnotation(a.annotation)
  ))

}

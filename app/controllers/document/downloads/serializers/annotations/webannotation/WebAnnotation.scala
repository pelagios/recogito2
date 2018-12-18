package controllers.document.downloads.serializers.annotations.webannotation

import org.joda.time.DateTime
import play.api.mvc.{ AnyContent, Request }
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasDate
import services.annotation.Annotation
import services.generated.tables.records.DocumentFilepartRecord

case class WebAnnotation(filepart: DocumentFilepartRecord, recogitoBaseURI: String, annotation: Annotation)

object WebAnnotation extends HasDate {
  
  implicit def webAnnotationWrites(implicit request: Request[AnyContent]): Writes[WebAnnotation] = (
    (JsPath \ "@context").write[String] and
    (JsPath \ "id").write[String] and
    (JsPath \ "type").write[String] and
    (JsPath \ "generator").write[Generator] and
    (JsPath \ "generated").write[DateTime] and
    (JsPath \ "body").write[Seq[WebAnnotationBody]] and
    (JsPath \ "target").write[WebAnnotationTarget]
  )(a => (
    "http://www.w3.org/ns/anno.jsonld",
    s"${a.recogitoBaseURI}annotation/${a.annotation.annotationId}",
    "Annotation",
    Generator(a.recogitoBaseURI),
    DateTime.now,
    a.annotation.bodies.flatMap(b => WebAnnotationBody.fromAnnotationBody(b, a.recogitoBaseURI)),
    WebAnnotationTarget.fromAnnotation(a.filepart, a.annotation)
  ))

}

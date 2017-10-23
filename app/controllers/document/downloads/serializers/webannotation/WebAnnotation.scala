package controllers.document.downloads.serializers.webannotation

import models.HasDate
import models.annotation.Annotation
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class WebAnnotation(recogitoBaseURI: String, documentURI: String, annotation: Annotation)

object WebAnnotation extends HasDate {
  
  implicit val webAnnotationWrites: Writes[WebAnnotation] = (
    (JsPath \ "@context").write[String] and
    (JsPath \ "id").write[String] and
    (JsPath \ "type").write[String] and
    (JsPath \ "generator").write[String] and
    (JsPath \ "generated").write[DateTime] and
    (JsPath \ "body").write[Seq[WebAnnotationBody]]
  )(a => (
    "http://www.w3.org/ns/anno.jsonld",
    a.documentURI + "#" + a.annotation.annotationId.toString,
    "Annotation",
    a.recogitoBaseURI,
    DateTime.now,
    a.annotation.bodies.flatMap(b => WebAnnotationBody.fromAnnotationBody(b, a.recogitoBaseURI))
  ))

}

package controllers.document.downloads.serializers.annotations.annotationlist

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class AnnotationResource(text: String, on: String)

object AnnotationResource {

  implicit val annotationListWrites: Writes[AnnotationResource] = (
    (JsPath \ "@id").write[String] and
    (JsPath \ "@type").write[String] and
    (JsPath \ "motivation").write[String] and
    (JsPath \ "resource").write[TextResource]
  )(a => (
    "http://localhost:8887/coin/list/1", // TODO
    "oa:Annotation",
    "sc:painting",
    TextResource(a.text, a.on)
  ))

}

case class TextResource(chars: String, on: String)

object TextResource {

  implicit val textResourceWrites: Writes[TextResource] = (
    (JsPath \ "@type").write[String] and
    (JsPath \ "format").write[String] and
    (JsPath \ "chars").write[String]
  )(t => (
    "cnt:ContentAsText",
    "text/plain",
    t.chars
  ))

}
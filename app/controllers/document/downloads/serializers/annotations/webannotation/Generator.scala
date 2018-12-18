package controllers.document.downloads.serializers.annotations.webannotation

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Generator(recogitoBaseURI: String)

object Generator {

  implicit val generatorWrites: Writes[Generator] = (
    (JsPath \ "id").write[String] and
    (JsPath \ "type").write[String] and
    (JsPath \ "name").write[String] and
    (JsPath \ "homepage").write[String]
  )(g => (
    g.recogitoBaseURI,
    "Software",
    "Recogito",
    g.recogitoBaseURI
  ))

}
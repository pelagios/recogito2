package services.entity

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Description(description: String, language: Option[String] = None)

object Description {

  implicit val descriptionFormat: Format[Description] = (
    (JsPath \ "description").format[String] and
    (JsPath \ "language").formatNullable[String]
  )(Description.apply, unlift(Description.unapply))

}
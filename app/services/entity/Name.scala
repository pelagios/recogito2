package services.entity

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasNullableBoolean

case class Name(name: String, language: Option[String] = None, isTransliterated: Boolean = false, isHistoric: Boolean = false)

object Name extends HasNullableBoolean {

  implicit val literalFormat: Format[Name] = (
    (JsPath \ "name").format[String] and
    (JsPath \ "language").formatNullable[String] and
    (JsPath \ "is_romanized").formatNullable[Boolean]
      .inmap[Boolean](fromOptBool, toOptBool) and
    (JsPath \ "is_historic").formatNullable[Boolean]
      .inmap[Boolean](fromOptBool, toOptBool)
  )(Name.apply, unlift(Name.unapply))

}
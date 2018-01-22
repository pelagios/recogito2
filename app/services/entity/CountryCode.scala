package services.entity

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class CountryCode(code: String) {

  require(code.size == 2, s"Invalid country code: ${code} (must be two characters)")
  require(code.toUpperCase == code, s"Invalid country code: ${code} (must be uppercase)")

}

object CountryCode {

  implicit val countryCodeFormat: Format[CountryCode] =
    Format(
      JsPath.read[String].map(CountryCode(_)),
      Writes[CountryCode](c => JsString(c.code))
    )

}
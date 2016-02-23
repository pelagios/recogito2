package models

import java.util.Date
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AnnotationStatus(

  value: AnnotationStatus.Value,

  setBy: Option[String],

  setAt: Option[Date]

)

object AnnotationStatus extends Enumeration {

  val UNVERIFIED = Value("UNVERIFIED")

  val VERIFIED = Value("VERIFIED")

  /** JSON serialization **/ 
  implicit val annotationStatusWrites: Writes[AnnotationStatus] = (
    (JsPath \ "value").write[String] ~
    (JsPath \ "set_by").writeNullable[String] ~
    (JsPath \ "set_at").writeNullable[Date]
  )(status => (
      status.value.toString,
      status.setBy,
      status.setAt))
  
}

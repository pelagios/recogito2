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

  /** JSON conversion **/
  implicit val annotationStatusValueFormat: Format[AnnotationStatus.Value] = 
    Format(
      (JsPath).read[String].map(AnnotationStatus.withName(_)),
      (JsPath).write[String].contramap((_.toString))
    ) 

  implicit val annotationStatusFormat: Format[AnnotationStatus] = (
    (JsPath \ "value").format[AnnotationStatus.Value] and
    (JsPath \ "set_by").formatNullable[String] and 
    (JsPath \ "set_at").formatNullable[Date]
  )(AnnotationStatus.apply, unlift(AnnotationStatus.unapply))
  
}

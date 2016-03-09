package models.annotation

import models.HasDate
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AnnotationStatus(

  value: AnnotationStatus.Value,

  setBy: Option[String],

  setAt: DateTime

)

object AnnotationStatus extends Enumeration with HasDate {

  val UNVERIFIED = Value("UNVERIFIED")

  val VERIFIED = Value("VERIFIED")

  /** JSON conversion **/
  implicit val annotationStatusValueFormat: Format[AnnotationStatus.Value] =
    Format(
      JsPath.read[String].map(AnnotationStatus.withName(_)),
      Writes[AnnotationStatus.Value](s => JsString(s.toString))
    )

  implicit val annotationStatusFormat: Format[AnnotationStatus] = (
    (JsPath \ "value").format[AnnotationStatus.Value] and
    (JsPath \ "set_by").formatNullable[String] and
    (JsPath \ "set_at").format[DateTime]
  )(AnnotationStatus.apply, unlift(AnnotationStatus.unapply))

}

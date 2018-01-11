package services.annotation

import services.HasDate
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AnnotationStatus(

  value: AnnotationStatus.Value,

  setBy: Option[String],

  setAt: DateTime

) {
  
  /** Shorthand that returns true if the status value are equal **/
  def equalsIgnoreModified(other: AnnotationStatus) =
    value == other.value
  
}

object AnnotationStatus extends Enumeration with HasDate {

  val UNVERIFIED = Value("UNVERIFIED")

  val VERIFIED = Value("VERIFIED")
  
  val NOT_IDENTIFIABLE = Value("NOT_IDENTIFIABLE")

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

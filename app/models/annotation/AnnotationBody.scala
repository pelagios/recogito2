package models.annotation

import models.HasDate
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AnnotationBody (

  hasType: AnnotationBody.Type,

  lastModifiedBy: Option[String],

  lastModifiedAt: DateTime,

  value: Option[String],

  uri: Option[String],
  
  status: Option[AnnotationStatus]

)

object AnnotationBody extends Enumeration with HasDate {

  type Type = Value

  val COMMENT = Value("COMMENT")

  val PERSON = Value("PERSON")

  val PLACE = Value("PLACE")

  val QUOTE = Value("QUOTE")

  val TAG = Value("TAG")

  val TRANSCRIPTION = Value("TRANSCRIPTION")

  /** JSON conversion **/
  implicit val annotationBodyTypeFormat: Format[AnnotationBody.Type] =
    Format(
      JsPath.read[String].map(AnnotationBody.withName(_)),
      Writes[AnnotationBody.Type](t => JsString(t.toString))
    )

  implicit val annotationBodyFormat: Format[AnnotationBody] = (
    (JsPath \ "type").format[AnnotationBody.Value] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime] and
    (JsPath \ "value").formatNullable[String] and
    (JsPath \ "uri").formatNullable[String] and
    (JsPath \ "status").formatNullable[AnnotationStatus]
  )(AnnotationBody.apply, unlift(AnnotationBody.unapply))

}

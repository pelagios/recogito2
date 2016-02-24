package models

import java.util.Date
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AnnotationBody (

  hasType: AnnotationBody.Type,

  createdBy: Option[String],

  createdAt: Date,

  lastModifiedBy: Option[String],

  lastModifiedAt: Option[Date],

  value: Option[String],

  uri: Option[String]

)

object AnnotationBody extends Enumeration {

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
      (JsPath).read[String].map(AnnotationBody.withName(_)),
      (JsPath).write[String].contramap((_.toString))
    ) 

  implicit val annotationBodyFormat: Format[AnnotationBody] = (
    (JsPath \ "type").format[AnnotationBody.Value] and
    (JsPath \ "created_by").formatNullable[String] and 
    (JsPath \ "created_at").format[Date] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").formatNullable[Date] and
    (JsPath \ "value").formatNullable[String] and
    (JsPath \ "uri").formatNullable[String]
  )(AnnotationBody.apply, unlift(AnnotationBody.unapply))

}



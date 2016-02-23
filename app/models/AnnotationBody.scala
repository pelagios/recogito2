package models

import java.util.Date
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AnnotationBody (

  hasType: AnnotationBody.Type,

  createdBy: String,

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
  
  /** JSON serialization **/
  implicit val annotationBodyWrites: Writes[AnnotationBody] = (
    (JsPath \ "type").write[String] ~
    (JsPath \ "created_by").write[String] ~
    (JsPath \ "created_at").write[Date] ~
    (JsPath \ "last_modified_by").writeNullable[String] ~
    (JsPath \ "last_modified_at").writeNullable[Date] ~
    (JsPath \ "value").writeNullable[String] ~
    (JsPath \ "uri").writeNullable[String]
  )(body => (
      body.hasType.toString,
      body.createdBy,
      body.createdAt,
      body.lastModifiedBy,
      body.lastModifiedAt,
      body.value,
      body.uri))

}



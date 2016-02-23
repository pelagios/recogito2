package models

import java.util.Date

case class AnnotationBody (

  hasType: AnnotationBody.Type,

  createdBy: String,

  createdAt: Date,

  quote: String,

  uri: String,

  text: String
)

object AnnotationBody extends Enumeration {

  type Type = Value

  val COMMENT = Value("COMMENT")

  val PERSON = Value("ENTITY_PERSON")

  val PLACE = Value("ENTITY_PLACE")

  val TAG = Value("TAG")

  val TRANSCRIPTION = Value("TRANSCRIPTION")

}

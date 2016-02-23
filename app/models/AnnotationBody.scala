package models

import java.util.Date

case class AnnotationBody (

  hasType: AnnotationBody.Type,

  createdBy: String,

  createdAt: Date,

  lastModifiedBy: String,

  lastModifiedAt: Date,

  value: String,

  uri: String

)

object AnnotationBody extends Enumeration {

  type Type = Value

  val COMMENT = Value("COMMENT")

  val PERSON = Value("PERSON")

  val PLACE = Value("PLACE")

  val QUOTE = Value("QUOTE")

  val TAG = Value("TAG")

  val TRANSCRIPTION = Value("TRANSCRIPTION")

}

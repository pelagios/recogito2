package models

import java.util.Date

case class AnnotationStatus(

  value: AnnotationStatus.Value,

  setBy: String,

  setAt: Date

)

object AnnotationStatus extends Enumeration {

  val UNVERIFIED = Value("UNVERIFIED")

  val VERIFIED = Value("VERIFIED")

}

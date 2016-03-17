package models.content

/** Expected error conditions during content upload **/
object ContentIdentificationFailures {
  
  sealed trait ContentIdentificationFailure
  
  case object UnsupportedContentType extends ContentIdentificationFailure
  
}
package models.content

object ContentIdentificationFailures {
  
  sealed trait ContentIdentificationFailure
  
  case object UnsupportedContentType extends ContentIdentificationFailure
    
}
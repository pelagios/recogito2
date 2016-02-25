package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

trait JsonDate {
  
  private val dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
  
  implicit val dateTimeFormat = 
    Format(
      Reads.jodaDateReads(dateFormat),
      Writes.jodaDateWrites(dateFormat)
    )
  
}
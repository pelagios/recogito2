package services

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

trait HasDate {

  private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ").withZone(DateTimeZone.UTC)

  implicit val dateTimeFormat =
    Format(
      JsPath.read[JsString].map { json =>
        formatter.parseDateTime(json.value)
      },
      
      Writes[DateTime] { dt =>
        Json.toJson(formatter.print(dt))
      }
    )
    
  /** Convenience method for external use, outside JSON serialization **/
  def formatDate(dt: DateTime) = formatter.print(dt.withZone(DateTimeZone.UTC))

}

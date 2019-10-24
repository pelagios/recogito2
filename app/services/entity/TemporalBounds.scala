package services.entity

import org.joda.time.{ DateTime, DateTimeZone }
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class TemporalBounds(from: DateTime, to: DateTime) {

  import TemporalBounds._

  // TODO make this smarter - for now, we'll just print to/from years
  override def toString() = if (from == to) {
    yearFormatter.print(from)
  } else {
    s"${yearFormatter.print(from)}/${yearFormatter.print(to)}"
  }

}

object TemporalBounds {

  // For convenience
  private val yearFormatter = DateTimeFormat.forPattern("yyyy").withZone(DateTimeZone.UTC)

  private val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZone(DateTimeZone.UTC)

  implicit val dateFormat =
    Format(
      JsPath.read[JsString].map { json =>
        dateFormatter.parseDateTime(json.value)
      },

      Writes[DateTime] { dt =>
        Json.toJson(dateFormatter.print(dt))
      }
    )

  /** Helper to produce a DateTime from a JsValue that's either an Int or a date string **/
  private def flexDateRead(json: JsValue): DateTime =
    json.asOpt[Int] match {
      case Some(year) => {
        new DateTime(DateTimeZone.UTC)
          .withDate(year, 1, 1)
          .withTime(0, 0, 0, 0)
      }
      case None => Json.fromJson[DateTime](json).get
    }

  /** Vice versa, generates an Int if the date is a year **/
  private def flexDateWrite(dt: DateTime): JsValue =
    if (dt.monthOfYear == 1 && dt.dayOfMonth == 1 && dt.minuteOfDay == 0)
      Json.toJson(dt.year.get)
    else
      Json.toJson(dt)

  implicit val temporalBoundsFormat: Format[TemporalBounds] = (
    (JsPath \ "from").format[JsValue].inmap[DateTime](flexDateRead, flexDateWrite) and
    (JsPath \ "to").format[JsValue].inmap[DateTime](flexDateRead, flexDateWrite)
  )(TemporalBounds.apply, unlift(TemporalBounds.unapply))

  def computeUnion(bounds: Seq[TemporalBounds]): TemporalBounds = {
    val from = bounds.map(_.from.getMillis).min
    val to = bounds.map(_.to.getMillis).max
    TemporalBounds(
      new DateTime(from, DateTimeZone.UTC),
      new DateTime(to, DateTimeZone.UTC))
  }

  def fromYears(from: Int, to: Int): TemporalBounds = {
    val f = new DateTime(DateTimeZone.UTC).withDate(from, 1, 1).withTime(0, 0, 0, 0)
    val t = new DateTime(DateTimeZone.UTC).withDate(to, 1, 1).withTime(0, 0, 0, 0)
    TemporalBounds(f, t)
  }

}

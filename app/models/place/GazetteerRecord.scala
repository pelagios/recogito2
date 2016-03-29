package models.place

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import models.HasDate

case class GazetteerRecord(  

  uri: String,

  sourceGazetteer: Gazetteer,

  title: String,
    
  placeTypes: Seq[String],
    
  descriptions: Seq[Description],
    
  names: Seq[Name],
    
  geometry: Option[Geometry],
    
  representativePoint: Option[Coordinate],
  
  temporalBounds: Option[TemporalBounds],
  
  closeMatches: Seq[String],
    
  exactMatches: Seq[String]
    
) {
  
  // For convenience
  lazy val allMatches = closeMatches ++ exactMatches 

}

case class Gazetteer(name: String)

case class Description(description: String, language: Option[String] = None)

case class Name(name: String, language: Option[String] = None)

case class TemporalBounds(from: DateTime, to: DateTime)

/** JSON (de)serialization **/

object GazetteerRecord {
  
  import Place._

  implicit val gazetteerRecordFormat: Format[GazetteerRecord] = (
    (JsPath \ "uri").format[String] and
    (JsPath \ "source_gazetteer").format[Gazetteer] and
    (JsPath \ "title").format[String] and
    (JsPath \ "place_types").format[Seq[String]] and
    (JsPath \ "descriptions").format[Seq[Description]] and
    (JsPath \ "names").format[Seq[Name]] and
    (JsPath \ "geometry").formatNullable[Geometry] and
    (JsPath \ "representative_point").formatNullable[Coordinate] and
    (JsPath \ "temporal_bounds").formatNullable[TemporalBounds] and
    (JsPath \ "close_matches").formatNullable[Seq[String]].inmap[Seq[String]](
        o => o.getOrElse(Seq.empty[String]),
        s => if (s.isEmpty) None else Some(s)
      ) and
    (JsPath \ "exact_matches").formatNullable[Seq[String]].inmap[Seq[String]](
        o => o.getOrElse(Seq.empty[String]),
        s => if (s.isEmpty) None else Some(s)
      )
  )(GazetteerRecord.apply, unlift(GazetteerRecord.unapply))
  
}

object Gazetteer {
  
  implicit val gazetteerFormat: Format[Gazetteer] =
    Format(
      JsPath.read[String].map(Gazetteer(_)),
      Writes[Gazetteer](t => JsString(t.name))
    )
    
}
    
object Description {
  
  implicit val descriptionFormat: Format[Description] = (
    (JsPath \ "description").format[String] and
    (JsPath \ "language").formatNullable[String]
  )(Description.apply, unlift(Description.unapply))
  
}

object Name {
  
  implicit val literalFormat: Format[Name] = (
    (JsPath \ "name").format[String] and
    (JsPath \ "language").formatNullable[String]
  )(Name.apply, unlift(Name.unapply))
  
}


object TemporalBounds extends HasDate {
 
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
  
}

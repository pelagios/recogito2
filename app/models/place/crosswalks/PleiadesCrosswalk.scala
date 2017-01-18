package models.place.crosswalks

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import models.{ HasDate, HasGeometry, HasNullableSeq }
import models.place.{ CountryCode, Description, Gazetteer, GazetteerRecord, Name }
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import models.place.TemporalBounds

object PleiadesCrosswalk extends BaseGeoJSONCrosswalk {
  
  private val PLEIADES = Gazetteer("Pleiades")
  
  private def computeTemporalBounds(names: Seq[PleiadesName]): Option[TemporalBounds] = {
    val startDate= names.flatMap(_.startDate)
    val endDate = names.flatMap(_.endDate)
    if (startDate.isEmpty || endDate.isEmpty)
      None 
    else
      Some(TemporalBounds.fromYears(startDate.min, endDate.max))
  }

  def fromJson(record: String): Option[GazetteerRecord] = super.fromJson[PleiadesRecord](record, { pleiades =>
    GazetteerRecord(
      pleiades.uri,
      PLEIADES,
      DateTime.now(),
      pleiades.history.headOption.map(_.modified),
      pleiades.title,
      pleiades.description.map(d => Seq(new Description(d))).getOrElse(Seq.empty[Description]),
      pleiades.names.flatMap(_.toNames),
      pleiades.features.headOption.map(_.geometry), // TODO compute union?
      pleiades.representativePoint,
      computeTemporalBounds(pleiades.names), // TODO temporalBounds
      pleiades.placeTypes,
      None, // country code
      None, // population
      Seq.empty[String], // TODO closeMatches
      Seq.empty[String]  // TODO exactMatches
    )
  })
  
}

case class HistoryRecord(modified: DateTime)

object HistoryRecord extends HasDate {
  
  implicit val historyRecordReads: Reads[HistoryRecord] = (JsPath \ "modified").read[DateTime].map(HistoryRecord(_))
  
}

case class PleiadesName(
  attested: Option[String],
  romanized: Option[String],
  language: Option[String],
  startDate: Option[Int],
  endDate: Option[Int]
) {
  
  // BAD bad Pleiades!
  lazy val normalizedLanguage = language match {
    case Some(language) if !language.trim.isEmpty => Some(language)
    case _ => None
  }
    
  lazy val toNames = Seq(attested, romanized).flatten.filter(!_.isEmpty).map(Name(_, language))
  
}

object PleiadesName {
  
   implicit val pleiadesNameReads: Reads[PleiadesName] = (
    (JsPath \ "attested").readNullable[String] and
    (JsPath \ "romanized").readNullable[String] and
    (JsPath \ "language").readNullable[String] and
    (JsPath \ "start").readNullable[Int] and
    (JsPath \ "end").readNullable[Int]
  )(PleiadesName.apply _) 
}

case class PleiadesRecord(

  uri: String,

  history: Seq[HistoryRecord],
  
  title: String,
  
  description: Option[String],
  
  names: Seq[PleiadesName],
  
  features: Seq[Feature],
  
  representativePoint: Option[Coordinate],
    
  placeTypes: Seq[String]
  
  // TODO close matches
  
  // TODO exact matches
    
)

object PleiadesRecord extends HasGeometry {
  
  implicit val pleiadesRecordReads: Reads[PleiadesRecord] = (
    (JsPath \ "uri").read[String] and
    (JsPath \ "history").read[Seq[HistoryRecord]] and
    (JsPath \ "title").read[String] and
    (JsPath \ "description").readNullable[String] and
    (JsPath \ "names").readNullable[Seq[PleiadesName]].map(_.getOrElse(Seq.empty[PleiadesName])) and
    (JsPath \ "features").readNullable[Seq[Feature]].map(_.getOrElse(Seq.empty[Feature])) and
    (JsPath \ "reprPoint").readNullable[Coordinate] and
    (JsPath \ "placeTypes").readNullable[Seq[String]].map(_.getOrElse(Seq.empty[String]))
  )(PleiadesRecord.apply _)
  
}


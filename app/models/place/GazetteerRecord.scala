package models.place

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import models.{ HasDate, HasGeometry, HasNullableSeq }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class GazetteerRecord(

  uri: String,

  sourceGazetteer: Gazetteer,

  lastChangedAt: DateTime,

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

  /** Returns true if there is a connection between the two records.
    *
    * A connection can result from one of the following three causes:
    * - this record lists the other record's URI as a close- or exactMatch
    * - the other record lists this record's URI as a close- or exactMatch
    * - both records share at least one close/exactMatch URI
    */
  def isConnectedWith(other: GazetteerRecord): Boolean =
    allMatches.contains(other.uri) ||
    other.allMatches.contains(uri) ||
    allMatches.exists(matchURI => other.allMatches.contains(matchURI))

  /** An 'equals' method that ignores the lastChangedAt property **/
  def equalsIgnoreLastChanged(other: GazetteerRecord): Boolean =
    throw new Exception("Implement me!")

}

case class Gazetteer(name: String)

case class Description(description: String, language: Option[String] = None)

case class Name(name: String, language: Option[String] = None)

/** JSON (de)serialization **/

object GazetteerRecord extends HasDate with HasGeometry with HasNullableSeq {

  implicit val gazetteerRecordFormat: Format[GazetteerRecord] = (
    (JsPath \ "uri").format[String] and
    (JsPath \ "source_gazetteer").format[Gazetteer] and
    (JsPath \ "last_changed_at").format[DateTime] and
    (JsPath \ "title").format[String] and
    (JsPath \ "place_types").formatNullable[Seq[String]]
      .inmap[Seq[String]](fromOptSeq[String], toOptSeq[String]) and
    (JsPath \ "descriptions").formatNullable[Seq[Description]]
      .inmap[Seq[Description]](fromOptSeq[Description], toOptSeq[Description]) and
    (JsPath \ "names").formatNullable[Seq[Name]]
      .inmap[Seq[Name]](fromOptSeq[Name], toOptSeq[Name]) and
    (JsPath \ "geometry").formatNullable[Geometry] and
    (JsPath \ "representative_point").formatNullable[Coordinate] and
    (JsPath \ "temporal_bounds").formatNullable[TemporalBounds] and
    (JsPath \ "close_matches").formatNullable[Seq[String]]
      .inmap[Seq[String]](fromOptSeq[String], toOptSeq[String]) and
    (JsPath \ "exact_matches").formatNullable[Seq[String]]
      .inmap[Seq[String]](fromOptSeq[String], toOptSeq[String])
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

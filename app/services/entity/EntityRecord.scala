package services.entity

import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.{HasDate, HasGeometry, HasNullableSeq, HasNullableBoolean}

case class EntityRecord (

  /** Canonical URI **/
  uri: String,

  /** Authority identifier **/
  sourceAuthority: String,

  /** Time the record was last synchronized from a current authority file dump **/
  lastSyncedAt: DateTime,

  /** Time the record was last changed, according to the authority file **/
  lastChangedAt: Option[DateTime],

  /** A 'title' for the record to use for screen display **/
  title: String,

  /** List of descriptions **/
  descriptions: Seq[Description],

  /** List of names **/
  names: Seq[Name],

  /** Geometry **/
  geometry: Option[Geometry],

  /** A representative point (not necessarily same as centroid) **/
  representativePoint: Option[Coordinate],

  /** ISO country code **/
  countryCode: Option[CountryCode],

  /** The temporal bounds of this record **/
  temporalBounds: Option[TemporalBounds],

  /** Subject tags assigned by the authority file **/
  subjects: Seq[String],

  /** An optional priority metric, to be used for sorting in the UI.
    * Could, e.g. be the population count for a PLACE entity
    */
  priority: Option[Long],

  /** close/exactMatch URIs etc. **/
  links: Seq[Link]

) {

  /** For convenience: all close and exact match URIs **/
  lazy val directMatches =
    links.filter(l => l.linkType == LinkType.CLOSE_MATCH || l.linkType == LinkType.EXACT_MATCH)
      .map(_.uri)

  def getLinks(t: LinkType.Value) =
    links.filter(_.linkType == t)

  /** Returns true if there is a connection between the two records.
    *
    * A connection can result from one of the following three causes:
    * - this record lists the other record's URI as a link
    * - the other record lists this record's URI as a link
    * - both records share at least one link
    */
  def isConnectedWith(other: EntityRecord): Boolean = {
    val thisIdentifiers = (uri :+ directMatches).toSet
    val otherIdentifiers = (other.uri :+ other.directMatches).toSet
    thisIdentifiers.intersect(otherIdentifiers).size > 0
  }
  
  /** Utility to create a cloned record, with all URIs normalized **/
  lazy val normalize = copy(
     uri = EntityRecord.normalizeURI(uri),
     links = links.map(_.normalize))

}

object EntityRecord extends HasDate with HasGeometry with HasNullableSeq {

  /** Utility method to normalize a URI to a standard format
    *
    * Removes '#this' suffixes (used by Pleiades) and, by convention, trailing slashes.
    */
  def normalizeURI(uri: String) = {
    val noThis = if (uri.indexOf("#this") > -1) uri.substring(0, uri.indexOf("#this")) else uri
    val httpOnly = noThis.replace("https://", "http://")
    if (httpOnly.endsWith("/"))
      httpOnly.substring(0, noThis.size - 1)
    else
      httpOnly
  }

  /** JSON (de)serialization **/
  implicit val entityRecordFormat: Format[EntityRecord] = (
    (JsPath \ "uri").format[String] and
    (JsPath \ "source_authority").format[String] and
    (JsPath \ "last_synced_at").format[DateTime] and
    (JsPath \ "last_changed_at").formatNullable[DateTime] and
    (JsPath \ "title").format[String] and
    (JsPath \ "descriptions").formatNullable[Seq[Description]]
      .inmap[Seq[Description]](fromOptSeq[Description], toOptSeq[Description]) and
    (JsPath \ "names").formatNullable[Seq[Name]]
      .inmap[Seq[Name]](fromOptSeq[Name], toOptSeq[Name]) and
    (JsPath \ "geometry").formatNullable[Geometry] and
    (JsPath \ "representative_point").formatNullable[Coordinate] and
    (JsPath \ "country_code").formatNullable[CountryCode] and
    (JsPath \ "temporal_bounds").formatNullable[TemporalBounds] and
    (JsPath \ "subjects").formatNullable[Seq[String]]
      .inmap[Seq[String]](fromOptSeq[String], toOptSeq[String]) and
    (JsPath \ "priority").formatNullable[Long] and
    (JsPath \ "links").formatNullable[Seq[Link]]
      .inmap[Seq[Link]](fromOptSeq[Link], toOptSeq[Link])
  )(EntityRecord.apply, unlift(EntityRecord.unapply))

}

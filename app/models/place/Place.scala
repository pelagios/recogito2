package models.place

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import models.{ HasDate, HasGeometry }
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Place (
    
  /** The ID equals the URI of the gazetteer record added first **/
  id: String,
    
  /** A representative geometry - e.g. that of a 'preferred gazetteer', or a union **/ 
  representativeGeometry: Option[Geometry],
    
  /** A representative point **/
  representativePoint: Option[Coordinate],
  
  /** Union of the temporal bounds of all gazetteer records **/
  temporalBoundsUnion: Option[TemporalBounds],
  
  /** The gazetteer records that define that place **/
  isConflationOf: Seq[GazetteerRecord]
  
) {
  
  /** URIs of all gazetteer records that define that place **/
  lazy val uris: Seq[String] = isConflationOf.map(_.uri)

  /** List of the gazetteers that define that place **/
  lazy val sourceGazetteers: Seq[Gazetteer] = isConflationOf.map(_.sourceGazetteer)
  
  /** List of titles **/   
  lazy val titles: Seq[String] = isConflationOf.map(_.title)
      
  /** Descriptions assigned to this place as Map[description -> list of gazetteers including the description] **/
  lazy val descriptions =
    isConflationOf
      .flatMap(g => g.descriptions.map((_, g.sourceGazetteer)))
      .groupBy(_._1)
      .map { case (description, s) => (description -> s.map(_._2)) }.toMap
      
  /** Names assigned to this place as Map[name -> list of gazetteers including the name] **/
  lazy val names=
    isConflationOf
      .flatMap(g => g.names.map((_, g.sourceGazetteer)))
      .groupBy(_._1)
      .map { case (name, s) => (name -> s.map(_._2)) }.toMap

  /** Place types assigned to this place as Map[placeType -> list of gazetteers including the type] **/
  lazy val placeTypes =
    isConflationOf
      .flatMap(g => g.placeTypes.map((_, g.sourceGazetteer)))
      .groupBy(_._1)
      .map { case (placeType, s) => (placeType -> s.map(_._2)) }.toMap
      
  /** All close matches assigned to this place by source gazetteers **/
  lazy val closeMatches = isConflationOf.flatMap(_.closeMatches)
  
  /** All exact matches assigned to this place by source gazetteers **/
  lazy val exactMatches = isConflationOf.flatMap(_.exactMatches)
  
  /** For covenience **/
  lazy val allMatches: Seq[String] = closeMatches ++ exactMatches
  
}

object Place extends HasGeometry {
  
  implicit val placeFormat: Format[Place] = (
    (JsPath \ "id").format[String] and
    (JsPath \ "representative_geometry").formatNullable[Geometry] and
    (JsPath \ "representative_point").formatNullable[Coordinate] and
    (JsPath \ "temporal_bounds_union").formatNullable[TemporalBounds] and
    (JsPath \ "is_conflation_of").format[Seq[GazetteerRecord]]
  )(Place.apply, unlift(Place.unapply))
  
}

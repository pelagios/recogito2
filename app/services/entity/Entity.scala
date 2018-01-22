package services.entity

import com.vividsolutions.jts.geom.{Coordinate, Envelope, Geometry }
import java.util.UUID
import services.{HasDate, HasGeometry}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Entity (

  /** An ID used internally for the union index record **/
  unionId: UUID,

  /** The entity type **/
  entityType: EntityType,

  /** A 'title' for the record to use for screen display **/
  title: String,

  /** A representative geometry - e.g. that of a 'preferred gazetteer', or a union **/
  representativeGeometry: Option[Geometry],

  /** A representative point **/
  representativePoint: Option[Coordinate],

  /** Union of the temporal bounds of all entity records **/
  temporalBoundsUnion: Option[TemporalBounds],

  /** The gazetteer records that define that place **/
  isConflationOf: Seq[EntityRecord],

  /** Bounding box, stored along with the entity (if any) **/
  private val storedBBox: Option[Envelope] = None

) {

  /** Shorthand: URIs of all entity records **/
  lazy val uris: Seq[String] = isConflationOf.map(_.uri)

  /** Shorthand: list of the authority identifiers that define this entity **/
  lazy val sourceAuthorities: Seq[String] = isConflationOf.map(_.sourceAuthority)

  /** Shorthand: titles **/
  lazy val titles: Seq[String] = isConflationOf.map(_.title)

  /** Descriptions as Map[description -> list of sources including the description] **/
  lazy val descriptions =
    isConflationOf
      .flatMap(e => e.descriptions.map((_, e.sourceAuthority)))
      .groupBy(_._1)
      .map { case (description, s) => (description -> s.map(_._2)) }.toMap

  /** Names assigned to this entity as Map[name -> list of sources including the name] **/
  lazy val names =
    isConflationOf
      .flatMap(e => e.names.map((_, e.sourceAuthority)))
      .groupBy(_._1)
      .map { case (name, s) => (name -> s.map(_._2)) }.toMap

  /** Subject tags as Map[subject -> list of sources including the subject] **/
  lazy val subjects =
    isConflationOf
      .flatMap(e => e.subjects.map((_, e.sourceAuthority)))
      .groupBy(_._1)
      .map { case (subject, s) => (subject -> s.map(_._2)) }.toMap
  
  /** Returns either the pre-computed bbox, or computes a new from the geometry **/
  lazy val bbox = storedBBox match {
    case Some(env) => Some(env)
    case None => representativeGeometry.map(_.getEnvelopeInternal)
  }

}

object Entity extends HasGeometry {
    
  // Although this means a bit more code, we use separate Reader/Writer
  // so we can specifically handle the bbox field
  implicit val entityReads: Reads[Entity] = (
    (JsPath \ "union_id").read[UUID] and
    (JsPath \ "entity_type").read[EntityType] and
    (JsPath \ "title").read[String] and
    (JsPath \ "representative_geometry").readNullable[Geometry] and
    (JsPath \ "representative_point").readNullable[Coordinate] and
    (JsPath \ "temporal_bounds_union").readNullable[TemporalBounds] and
    (JsPath \ "is_conflation_of").read[Seq[EntityRecord]] and
    (JsPath \ "bbox").readNullable[Envelope]
  )(Entity.apply _)
  
  implicit val entityWrites: Writes[Entity] = (
    (JsPath \ "union_id").write[UUID] and
    (JsPath \ "entity_type").write[EntityType] and
    (JsPath \ "title").write[String] and
    (JsPath \ "representative_geometry").writeNullable[Geometry] and
    (JsPath \ "representative_point").writeNullable[Coordinate] and
    (JsPath \ "bbox").writeNullable[Envelope] and
    (JsPath \ "temporal_bounds_union").writeNullable[TemporalBounds] and
    (JsPath \ "is_conflation_of").write[Seq[EntityRecord]]
  )(entity => (
      entity.unionId,
      entity.entityType,
      entity.title,
      entity.representativeGeometry,
      entity.representativePoint,
      entity.bbox,
      entity.temporalBoundsUnion,
      entity.isConflationOf
  ))

}

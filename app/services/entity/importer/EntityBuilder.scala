package services.entity.importer

import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import services.entity._
import java.util.UUID

object EntityBuilder {
  
  /** Picks a 'most detailed' record we can use for representative metadata 
    *
    * For the time being, we'll use number of names + number of links as
    * a measure. In addition, we'll boost the score if matches contain
    * a Wikidata or Wikipedia reference.
    */
  private def getMostDetailedRecord(records: Seq[EntityRecord]) =
    records.sortBy { record =>
      val score = record.directMatches.size + record.names.size
      val boostWikidata = record.directMatches.contains("www.wikidata.org")
      val boostWikipedia = record.directMatches.contains("wikipedia.org")
      
      val boost = 
        if (boostWikidata && boostWikipedia) 1.44
        else if (boostWikidata || boostWikipedia) 1.2
        else 1
        
      // Sort descending
      - score * boost
    }.head
    
  /** Robust way to compute a centroid from a geometry.
    *
    * Unfortunately, JTS will generate the following "valid" centroid
    * for MultiPoly geometries: Coordinate(NaN, NaN, NaN)
    * This method works around this... JTS feature.
    */
  private[importer] def getCentroid(geom: Geometry) = {
    val centroid = geom.getCentroid.getCoordinate
    if (centroid.x.isNaN || centroid.y.isNaN) {
      val coords = geom.getCoordinates
      val x = coords.map(_.x).sum / coords.size
      val y = coords.map(_.y).sum / coords.size
      new Coordinate(x, y)
    } else {
      centroid
    }
  }
          
  /** A set of conventions to select reasonable 'representative geometry' for an entity **/
  private def getPreferredLocation(records: Seq[EntityRecord]): (Option[Geometry], Option[Coordinate]) = {    
    // Rule 1: prefer DARE for representative point
    val dareRecord =
      records.filter(_.uri.contains("dare.ht.lu.se/places")).headOption

    val representativePoint = dareRecord.flatMap(_.representativePoint) match {
      case Some(point) =>
        Some(point) // Use DARE

      case None =>
        records.flatMap(_.representativePoint).headOption // Use first Available
    }

    // Rule 2: select the most high-res geometry, but fall back to DARE if it's a point
    val representativeGeometry = records.flatMap(_.geometry)
      .sortBy(_.getCoordinates().size).reverse
      .headOption.map { geom =>
        if (geom.getCoordinates.size == 1)
          dareRecord.flatMap(_.geometry).getOrElse(geom)
        else
          geom
      }

    val (geom, point) = (representativeGeometry, representativePoint) match {
      case (Some(g), Some(pt)) =>
        // Both defined - we're fine
        (Some(g), Some(pt))

      case (None, None) =>
        // Nothing defined - fair enough
        (None, None)

      case (Some(g), None) =>
        // Only geometry - use centroid for point
        (Some(g), Some(getCentroid(g)))

      case (None, Some(pt)) =>
        // Only point - add a point geometry
        (Some(new GeometryFactory().createPoint(pt)), Some(pt))
    }

    (geom, point)
  }

  /** Joins the given record and entities to a single entity.
    *
    * By convention, the "biggest" entity (the one with the largest number of 
    * records) determines the internal ID.
    */
  def join(normalizedRecord: EntityRecord, entities: Seq[Entity], entityType: EntityType): Entity = {
    val topRecord = entities.sortBy(- _.isConflationOf.size).headOption
    val internalId = topRecord.map(_.unionId).getOrElse(UUID.randomUUID)
    val allRecords = entities.flatMap(_.isConflationOf) :+ normalizedRecord
    fromRecords(allRecords, entityType, internalId)
  }
  
  def fromRecords(records: Seq[EntityRecord], entityType: EntityType, unionId: UUID) = {
    val mostDetailed = getMostDetailedRecord(records)

    val (geom, point) = getPreferredLocation(records)

    val temporalBoundsUnion = records.flatMap(_.temporalBounds) match {
      case bounds if bounds.size > 0 => Some(TemporalBounds.computeUnion(bounds))
      case _ => None
    }

    Entity(
      unionId,
      entityType,
      mostDetailed.title,
      geom, point,
      temporalBoundsUnion,
      records)
  }
  
}
package models.place

import models.place.GazetteerUtils._
import play.api.Logger
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import scala.language.postfixOps

trait PlaceImporter { self: PlaceStore =>

  // Maximum number of URIs we will concatenate to an OR query
  private def MAX_URIS_IN_QUERY = 100

  // Maximum number of times a gazetteer record or place link update will be retried in case of failure
  private def MAX_RETRIES = 10

  /** Retrieves all places in the store that will be affected from adding the record **/
  private[place] def getAffectedPlaces(normalizedRecord: GazetteerRecord)(implicit context: ExecutionContext): Future[Seq[(Place, Long)]] = {
    // We need to query for this record's URI as well as all close/exactMatchURIs
    val uris = normalizedRecord.uri +: normalizedRecord.allMatches

    // Protective measure - we don't really expect this to happen
    if (uris.size > MAX_URIS_IN_QUERY)
      throw new Exception("Maximum allowed number of close/exactMatch URIs exceeded by " + normalizedRecord.uri)

    findByPlaceOrMatchURIs(uris)
  }

  private def join(normalizedRecord: GazetteerRecord, places: Seq[Place]): Place = {
    // The general rule is that the "biggest place" (with highest number of gazetteer records) determines
    // ID and title of the conflated places
    val affectedPlacesSorted = places.sortBy(- _.isConflationOf.size)
    val definingPlace = affectedPlacesSorted.headOption
    val allRecords = places.flatMap(_.isConflationOf) :+ normalizedRecord

    // Temporal bounds are computed as the union of all gazetteer records
    def temporalBoundsUnion(bounds: Seq[TemporalBounds]): Option[TemporalBounds] =
      if (bounds.isEmpty)
        None
      else
        Some(TemporalBounds.computeUnion(bounds))

    Place(
      definingPlace.map(_.id).getOrElse(normalizedRecord.uri),
      GazetteerUtils.collectLabels(allRecords),
      definingPlace.map(_.geometry).getOrElse(normalizedRecord.geometry), // TODO implement rules for preferred geometry
      definingPlace.map(_.representativePoint).getOrElse(normalizedRecord.representativePoint), // TODO implement rules for preferred point
      temporalBoundsUnion((places.map(_.temporalBounds) :+ normalizedRecord.temporalBounds).flatten),
      allRecords
    )
  }

  /** Conflates a list of M gazetteer records into N places (with N <= M) **/
  private[place] def conflate(normalizedRecords: Seq[GazetteerRecord], places: Seq[Place] = Seq.empty[Place]): Seq[Place] = {

    // Conflates a single record
    def conflateOneRecord(r: GazetteerRecord, p: Seq[Place]): Seq[Place] = {
      val connectedPlaces = p.filter(_.isConflationOf.exists(_.isConnectedWith(r)))
      val unconnectedPlaces = places.diff(connectedPlaces)
      join(r, connectedPlaces) +: unconnectedPlaces
    }

    if (normalizedRecords.isEmpty) {
      places
    } else {
      val conflatedPlaces = conflateOneRecord(normalizedRecords.head, places)
      conflate(normalizedRecords.tail, conflatedPlaces)
    }
  }

  private def importRecord(record: GazetteerRecord)(implicit context: ExecutionContext): Future[Boolean] = {

    // Fetches affected places from the store and computes the new conflation
    def conflateAffectedPlaces(normalizedRecord: GazetteerRecord): Future[(Seq[(Place, Long)], Seq[Place])] = {
      getAffectedPlaces(normalizedRecord).map(p => {
        // Sorted affected places by no. of gazetteer records
        val affectedPlaces = p.sortBy(- _._1.isConflationOf.size)

        val affectedRecords =
          affectedPlaces
            .flatMap(_._1.isConflationOf) // all gazetteer records contained in the affected places
            .filter(_.uri != record.uri) // This record might update to an existing record!

        val conflated = conflate(affectedRecords :+ normalizedRecord)

        // Pass back places before and after conflation
        (affectedPlaces, conflated)
      })
    }

    // Stores the newly conflated places to the store
    def storeUpdatedPlaces(placesAfter: Seq[Place]): Future[Seq[Place]] =
      Future.sequence {
        placesAfter.map(place => insertOrUpdatePlace(place).map((place, _)))
      } map { _.filter(!_._2._1).map(_._1) }

    // Deletes the places that no longer exist after the conflation from the store
    def deleteMergedPlaces(placesBefore: Seq[(Place, Long)], placesAfter: Seq[Place]): Future[Seq[String]] =
      Future.sequence {
        // List of associations (Record URI, Parent PlaceID) before conflation
        val recordToParentMappingBefore = placesBefore.flatMap(t =>
          t._1.isConflationOf.map(record => (record.uri, t._1.id)))

        // List of associations (Record URI, Parent PlaceID) after conflation
        val recordToParentMappingAfter = placesAfter.flatMap(place =>
          place.isConflationOf.map(record => (record.uri, place.id)))

        // We need to delete all places that appear before, but not after the conflation
        val placeIdsBefore = recordToParentMappingBefore.map(_._2).distinct
        val placeIdsAfter = recordToParentMappingAfter.map(_._2).distinct

        val toDelete = placeIdsBefore diff placeIdsAfter
        toDelete.map(id => deletePlace(id).map((id, _)))
      } map { _.filter(!_._2).map(_._1) }

    for {
      (placesBefore, placesAfter) <- conflateAffectedPlaces(normalizeRecord(record))
      failedUpdates <- storeUpdatedPlaces(placesAfter)
      // Only do deletes if we know updates were stored first!
      failedDeletes <- if (failedUpdates.isEmpty) deleteMergedPlaces(placesBefore, placesAfter) else Future.successful(Seq.empty[String])
    } yield failedUpdates.isEmpty && failedDeletes.isEmpty

    // TODO Now we need to re-write the GeoTags
    // TODO - identify which record-to-place-mappings have changed
    // TODO - fetch those from the store
    // TODO - update them to the new value
    // TODO Note: this is a running system, users may have changed the
    // TODO the link - use optimistic locking, re-run failures
  }

  /** TODO chain the Futures properly instead of using Await! **/
  def importRecords(records: Seq[GazetteerRecord], retries: Int = MAX_RETRIES)(implicit context: ExecutionContext): Future[Seq[GazetteerRecord]] =
    Future {
      records.map { record =>
        try {
          (record, Await.result(importRecord(record), 5 seconds))
        } catch { case t: Throwable =>
          t.printStackTrace()
          (record, false)
        }
      }.filter(!_._2).map(_._1)
    }.flatMap { failedRecords =>
      if (failedRecords.size > 0 && retries > 0) {
        Logger.warn(failedRecords.size + " gazetteer records failed to import - retrying")
        importRecords(failedRecords, retries - 1)
      } else {
        Logger.info("Successfully imported " + (records.size - failedRecords.size) + " records")
        if (failedRecords.size > 0)
          Logger.error(failedRecords.size + " gazetteer records failed without recovery")
        else
          Logger.info("No failed imports")
        Future.successful(failedRecords)
      }
    }

}

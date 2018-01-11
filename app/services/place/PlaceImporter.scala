package services.place

import services.geotag.GeoTagStore
import play.api.Logger
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._

trait PlaceImporter { self: PlaceStore with GeoTagStore =>

  // Maximum number of URIs we will concatenate to an OR query
  private def MAX_URIS_IN_QUERY = 100

  // Maximum number of times a gazetteer record or place link update will be retried in case of failure
  private def MAX_RETRIES = 5
  
  private val BACKOFF_MS = 1000

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

    val (representativeGeometry, representativePoint) = GazetteerRecord.getPreferredLocation(allRecords)
    
    Place(
      definingPlace.map(_.id).getOrElse(normalizedRecord.uri),
      representativeGeometry,
      representativePoint,
      temporalBoundsUnion((places.map(_.temporalBoundsUnion) :+ normalizedRecord.temporalBounds).flatten),
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

  def importRecord(record: GazetteerRecord)(implicit context: ExecutionContext): Future[Boolean] = {
    
    val startTime = System.currentTimeMillis()

    // Fetches affected places from the store and computes the new conflation
    def conflateAffectedPlaces(normalizedRecord: GazetteerRecord): Future[(Seq[(Place, Long)], Seq[Place])] = {
      getAffectedPlaces(normalizedRecord).map(p => {
        // Sorted affected places by no. of gazetteer records
        val affectedPlaces = p.sortBy(- _._1.isConflationOf.size)
        
        if (affectedPlaces.size > 1)
          Logger.info("Merging places " + affectedPlaces.map(_._1.id).mkString(", "))

        val records = {
          // All records contained in affectedPlaces
          val affectedRecords = affectedPlaces.flatMap(_._1.isConflationOf) 
          
          // This record might update an existing one - find out where it is in the list
          val replaceIdx = affectedRecords.indexWhere(_.uri == normalizedRecord.uri)
          if (replaceIdx < 0)
            // Nope, the record is new - just append
            affectedRecords :+ normalizedRecord
          else
            // Replace the old version in the list
            affectedRecords.patch(replaceIdx, Seq(normalizedRecord), 1)              
        }

        // Pass back places before and after conflation
        (affectedPlaces, conflate(records))
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

    val f = for {
      (placesBefore, placesAfter) <- conflateAffectedPlaces(GazetteerRecord.normalize(record))
      failedUpdates <- storeUpdatedPlaces(placesAfter)
      geotagRewriteSuccessful <- if (failedUpdates.isEmpty) rewriteGeoTags(placesBefore.map(_._1), placesAfter) else Future.successful(false)
      failedDeletes <- if (geotagRewriteSuccessful) deleteMergedPlaces(placesBefore, placesAfter) else Future.successful(Seq.empty[String])
    } yield failedUpdates.isEmpty && failedDeletes.isEmpty && geotagRewriteSuccessful
    
    f.map { success =>
      val took = System.currentTimeMillis - startTime
      if (took > 1000) {
        Logger.info(s"Indexing ${record.title} took ${took} ms")
        
        // Give ElasticSearch a break, or it will... break 
        Thread.sleep(5000) 
      }
      success 
    }.recover { case t: Throwable =>
      t.printStackTrace()
      false
    }
  }

  def importRecords(records: Seq[GazetteerRecord], retries: Int = MAX_RETRIES)(implicit ctx: ExecutionContext): Future[Seq[GazetteerRecord]] =
    records.foldLeft(Future.successful(Seq.empty[GazetteerRecord])) { case (f, next) =>
      f.flatMap { failed =>
        importRecord(next).map { success =>
          if (success) failed
          else next +: failed 
        }
      }
    } flatMap { failedRecords =>
      Logger.info(s"Imported ${(records.size - failedRecords.size)} records")
      if (failedRecords.size > 0 && retries > 0) {
        Logger.warn(s"${failedRecords.size} gazetteer records failed to import - retrying")
        
        // Start first retry immediately and then increases wait time for each subsequent retry 
        val backoff = (MAX_RETRIES - retries) * BACKOFF_MS  
        if (backoff > 0) {
          Logger.info(s"Waiting... ${backoff}ms")
          Thread.sleep(backoff)
        }
        
        Logger.debug("Retrying now.")
        importRecords(failedRecords, retries - 1)
      } else {
        if (failedRecords.size > 0) {
          Logger.error(s"${failedRecords.size} gazetteer records failed without recovery")
          failedRecords.foreach(record =>  Logger.error(record.toString))
        } else {
          Logger.info("No failed imports")
        }
        Future.successful(failedRecords)
      }
    }
  
  /** TODO chain the Futures properly instead of using Await! **
  def importRecords(records: Seq[GazetteerRecord], retries: Int = MAX_RETRIES)(implicit context: ExecutionContext): Future[Seq[GazetteerRecord]] =
    Future {
      records.map { record =>
        try {
          (record, Await.result(importRecord(record), 20.seconds))
        } catch { case t: Throwable =>
          t.printStackTrace()
          play.api.Logger.warn(t.getMessage)
          (record, false)
        }
      }.filter(!_._2).map(_._1)
    }.flatMap { failedRecords =>
      Logger.info("Imported " + (records.size - failedRecords.size) + " records") 
      if (failedRecords.size > 0 && retries > 0) {
        Logger.warn(failedRecords.size + " gazetteer records failed to import - retrying")
        importRecords(failedRecords, retries - 1)
      } else {
        if (failedRecords.size > 0)
          Logger.error(failedRecords.size + " gazetteer records failed without recovery: " + failedRecords.map(_.uri).mkString(", "))
        else
          Logger.info("No failed imports")
          
        Future.successful(failedRecords)
      }
    }*/

}

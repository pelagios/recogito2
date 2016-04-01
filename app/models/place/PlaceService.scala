package models.place

import GazetteerUtils._
import play.api.Logger

object PlaceService {
  
  // Maximum number of URIs we will concatenate to an OR query
  private def MAX_URIS_IN_QUERY = 100
  
  // Maximum number of times a gazetteer record or place link update will be retried in case of failure 
  private def MAX_RETRIES = 10
  
  private lazy val esStore = new ESPlaceStore()
  
  /** Retrieves all places in the store that will be affected from adding the record **/
  private[place] def getAffectedPlaces(normalizedRecord: GazetteerRecord, store: PlaceStore): Seq[Place] = {
    // We need to query for this record's URI as well as all close/exactMatchURIs
    val uris = normalizedRecord.uri +: normalizedRecord.allMatches
    
    // Protective measure - we don't really expect this to happen
    if (uris.size > MAX_URIS_IN_QUERY)
      throw new Exception("Maximum allowed number of close/exactMatch URIs exceeded by " + normalizedRecord.uri)
    
    store.findByPlaceOrMatchURIs(uris)
  }
  
  private def join(normalizedRecord: GazetteerRecord, places: Seq[Place]): Place = {
    // The general rule is that the "biggest place" (with highest number of gazetteer records) determines
    // ID and title of the conflated places
    val affectedPlacesSorted = places.sortBy(- _.isConflationOf.size)
    val definingPlace = affectedPlacesSorted.headOption
    
    // Temporal bounds are computed as the union of all gazetteer records
    def temporalBoundsUnion(bounds: Seq[TemporalBounds]): Option[TemporalBounds] =
      if (bounds.isEmpty)
        None 
      else
        Some(TemporalBounds.computeUnion(bounds))
      
    // TODO in case we're conflating more than one places, we will need to update PlaceReferences in the store accordingly
        
    Place(
      definingPlace.map(_.id).getOrElse(normalizedRecord.uri),
      definingPlace.map(_.title).getOrElse(normalizedRecord.title),
      definingPlace.map(_.geometry).getOrElse(normalizedRecord.geometry), // TODO implement rules for preferred geometry
      definingPlace.map(_.representativePoint).getOrElse(normalizedRecord.representativePoint), // TODO implement rules for preferred point
      temporalBoundsUnion((places.map(_.temporalBounds) :+ normalizedRecord.temporalBounds).flatten),
      places.toSeq.flatMap(_.isConflationOf) :+ normalizedRecord
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
  
  private def importRecord(record: GazetteerRecord, store: PlaceStore): Boolean = {    
    val normalizedRecord = normalizeRecord(record)
    
    // All places that will be affected by the import, sorted by no. of gazetteer records
    val affectedPlaces = getAffectedPlaces(normalizedRecord, store).sortBy(- _.isConflationOf.size)

    val affectedRecords = 
      affectedPlaces
        .flatMap(_.isConflationOf) // all gazetteer records contained in the affected places
        .filter(_.uri != record.uri) // This record might update to an existing record!
        
    val conflated = conflate(affectedRecords :+ normalizedRecord)
    
    // Add (or update) the newly conflated places
    conflated.foreach(p => store.insertOrUpdatePlace(p))
    
    // List of associations (Record URI, Parent PlaceID) before conflation
    val recordToParentMappingBefore = affectedPlaces.flatMap(place =>
      place.isConflationOf.map(record => (record.uri, place.id)))
      
    // List of associations (Record URI, Parent PlaceID) after conflation
    val recordToParentMappingAfter = conflated.flatMap(place =>
      place.isConflationOf.map(record => (record.uri, place.id)))
    
    // We need to delete places that appear before, but not after the conflation
    val placeIdsBefore = recordToParentMappingBefore.map(_._2).distinct
    val placeIdsAfter = recordToParentMappingAfter.map(_._2).distinct
    
    val toDelete = placeIdsBefore diff placeIdsAfter
    toDelete.foreach(id => store.deletePlace(id))
    
    // TODO Now we need to re-write the PlaceLinks
    // TODO - identify which record-to-place-mappings have changed
    // TODO - fetch those from the store
    // TODO - update them to the new value
    // TODO Note: this is a running system, users may have changed the
    // TODO the link - use optimistic locking, re-run failures
    
    true // set to false in case of failure
  }
  
  def importRecords(records: Seq[GazetteerRecord], store: PlaceStore = esStore, retries: Int = MAX_RETRIES): Unit = {
    val failedRecords = records.foldLeft(Seq.empty[GazetteerRecord]) { (failed, record) =>  
      val success = importRecord(record, store)
      if (success)
        failed
      else
        failed :+ record
    }
    
    if (failedRecords.size > 0 && retries > 0)
      importRecords(failedRecords, store, retries - 1)
  }
  
  def totalPlaces(store: PlaceStore = esStore) = store.totalPlaces
  
  def findByURI(uri: String, store: PlaceStore = esStore) = store.findByURI(uri)
  
  def searchByName(query: String, store: PlaceStore = esStore) = store.searchByName(query)

}

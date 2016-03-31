package models.place

import GazetteerUtils._

object PlaceService {
  
  // Maximum number of URIs we will concatenate to an OR query
  private def MAX_URIS_IN_QUERY = 100
  
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
  
  private def importRecord(record: GazetteerRecord, store: PlaceStore) = {    
    val normalizedRecord = normalizeRecord(record)
    
    // All places that will be affected by the import, sorted by no. of gazetteer records
    val affectedPlaces = getAffectedPlaces(normalizedRecord, store).sortBy(- _.isConflationOf.size)

    val affectedRecords = 
      affectedPlaces
        .flatMap(_.isConflationOf) // all gazetteer records contained in the affected places
        .filter(_.uri != record.uri) // This record might be an update to an existing record!

    // "Re-conflated" places after adding the record
    val conflated = conflate(affectedRecords :+ normalizedRecord)
    
    // Add (or update) the newly conflated places
    conflated.foreach(p => store.insertOrUpdatePlace(p))
    
    // Affected places that are not among the conflated places need to be deleted
    val toDelete = affectedPlaces.map(_.id).filter(uri => !conflated.map(_.id).contains(uri))
      
    toDelete.foreach(id => store.deletePlace(id))
  }
  
  def importRecords(records: Seq[GazetteerRecord], store: PlaceStore = esStore) = {
    records.foreach(importRecord(_, store))
  }
  
  def totalPlaces(store: PlaceStore = esStore) = store.totalPlaces
  
  def findByURI(uri: String, store: PlaceStore = esStore) = store.findByURI(uri)
  
  def searchByName(query: String, store: PlaceStore = esStore) = store.searchByName(query)

}

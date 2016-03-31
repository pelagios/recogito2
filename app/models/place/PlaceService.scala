package models.place

import GazetteerUtils._
import play.api.Logger

object PlaceService {
  
  // Maximum number of URIs we will concatenate to an OR query
  private def MAX_URIS_IN_QUERY = 100
  
  private lazy val esStore = new ESPlaceStore()
  
  /** Retrieves all places in the store that will be affected from adding the record **/
  private[place] def getAffectedPlaces(record: GazetteerRecord, store: PlaceStore): Seq[Place] = {
    // We need to query for this record's URI as well as all close/exactMatchURIs
    val uris = record.uri +: record.allMatches
    
    // Protective measure - we don't really expect this to happen
    if (uris.size > MAX_URIS_IN_QUERY)
      throw new Exception("Maximum allowed number of close/exactMatch URIs exceeded by " + record.uri)
    
    store.findByPlaceOrMatchURIs(uris)
  }
  
  private[place] def conflatePlaces(normalizedRecord: GazetteerRecord, places: Seq[Place]): Seq[Place] = {
    // The general rule is that the "biggest place" (with highest number of gazetteer records) determines
    // ID and title of the conflated places
    val definingPlace = places.sortBy(_.isConflationOf.size).headOption
    
    // Temporal bounds are computed as the union of all gazetteer records
    def temporalBoundsUnion(bounds: Seq[TemporalBounds]): Option[TemporalBounds] =
      if (bounds.isEmpty)
        None 
      else
        Some(TemporalBounds.computeUnion(bounds))
      
    // TODO in case we're conflating more than one places, we will need to update PlaceReferences in the store accordingly
        
    val place = Place(
      definingPlace.map(_.id).getOrElse(normalizedRecord.uri),
      definingPlace.map(_.title).getOrElse(normalizedRecord.title),
      definingPlace.map(_.geometry).getOrElse(normalizedRecord.geometry), // TODO implement rules for preferred geometry
      definingPlace.map(_.representativePoint).getOrElse(normalizedRecord.representativePoint), // TODO implement rules for preferred point
      temporalBoundsUnion((places.map(_.temporalBounds) :+ normalizedRecord.temporalBounds).flatten),
      places.toSeq.flatMap(_.isConflationOf) :+ normalizedRecord
    )
    
    Seq(place)
  }
  
  private[place] def addGazetteerRecord(record: GazetteerRecord, store: PlaceStore) = {
    val normalizedRecord = normalizeRecord(record)
    
    // All places that will be affected from importing this record
    val affectedPlaces = getAffectedPlaces(normalizedRecord, store)
      
    // "Re-conflated" places after adding the record 
    val conflated = conflatePlaces(normalizedRecord, affectedPlaces)
    conflated.foreach(place => store.insertOrUpdatePlace(place))
  }
  
  def importGazetteerRecords(records: Seq[GazetteerRecord], store: PlaceStore = esStore) = {
    records.foreach(addGazetteerRecord(_, store))
  }
  
  def totalPlaces(store: PlaceStore = esStore) = store.totalPlaces
  
  def findByURI(uri: String, store: PlaceStore = esStore) = store.findByURI(uri)
  
  def searchByName(query: String, store: PlaceStore = esStore) = store.searchByName(query)

}

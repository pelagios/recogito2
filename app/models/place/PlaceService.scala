package models.place

import GazetteerUtils._
import play.api.Logger

object PlaceService {
  
  private lazy val esStore = new ESPlaceStore()
  
  // TODO speed this up by running one query with multiple URIs, rather than multiple queries with one 
  private def addGazetteerRecord(record: GazetteerRecord, store: PlaceStore) = {
    val normalizedRecord = normalizeRecord(record)
      
    if (store.findByURI(normalizedRecord.uri).isDefined) {
      
      // TODO gazetteer record already exists - update rather than add
      throw new Exception("TODO implement updates")
      
    } else {    
      // First, we query the store for places that list our record as close- or exactMatch
      val directMatchesIn = store.findByMatchURI(normalizedRecord.uri)
      
      // Next, the other way round: we query the store with this record's close- and exactMatches
      val maybeDirectMatchesOut = (normalizedRecord.allMatches).map(uri =>  {
        (uri, store.findByURI(uri))
      })
      
      // These are the matches we found in the store...
      val directMatchesOut = maybeDirectMatchesOut.filter(_._2.isDefined).flatMap(_._2)
      
      val directMatches = (directMatchesIn ++ directMatchesOut).toSet
      
      // ...and these are URIs for which we didn't find matches - we'll re-use those below!
      val unmatchedURIs = maybeDirectMatchesOut.filter(_._2.isEmpty).map(_._1)
        
      // Now let's use the unmatched URIs to query for indirect matches, i.e. URIs that 
      // both THIS record and EXISTING records list as match
      val indirectMatches = 
        unmatchedURIs
          .flatMap(uri => store.findByMatchURI(uri))
          .filter(!directMatches.contains(_)) // Don't double-count places that are already connected directly

      val allMatches = directMatches ++ indirectMatches
      allMatches.foreach(p => store.deletePlace(p.id))

      val conflatedPlace = conflate(normalizedRecord, allMatches, store)
      store.insertPlace(conflatedPlace)
    }
  }
  
  private def conflate(normalizedRecord: GazetteerRecord, places: Set[Place], store: PlaceStore): Place = {
    // The general rule is that the "biggest place" (with highest number of gazetteer records) determines
    // ID and title of the conflated places
    val definingPlace = places.toSeq.sortBy(_.isConflationOf.size).headOption
    
    // Temporal bounds are computed as the union of all gazetteer records
    def temporalBoundsUnion(bounds: Set[TemporalBounds]): Option[TemporalBounds] =
      if (bounds.isEmpty)
        None 
      else
        Some(TemporalBounds.computeUnion(bounds.toSeq))
      
    // TODO in case we're conflating more than one places, we will need to update PlaceReferences in the store accordingly
        
    Place(
      definingPlace.map(_.id).getOrElse(normalizedRecord.uri),
      definingPlace.map(_.title).getOrElse(normalizedRecord.title),
      definingPlace.map(_.geometry).getOrElse(normalizedRecord.geometry), // TODO implement rules for preferred geometry
      definingPlace.map(_.representativePoint).getOrElse(normalizedRecord.representativePoint), // TODO implement rules for preferred point
      temporalBoundsUnion((places.map(_.temporalBounds) + normalizedRecord.temporalBounds).flatten),
      places.toSeq.flatMap(_.isConflationOf) :+ normalizedRecord
    )
  }
  
  def importGazetteerRecords(records: Seq[GazetteerRecord], store: PlaceStore = esStore) = {
    records.foreach(addGazetteerRecord(_, store))
  }
  
  def totalPlaces(store: PlaceStore = esStore) = store.totalPlaces
  
  def findByURI(uri: String, store: PlaceStore = esStore) = store.findByURI(uri)
  
  def findByMatchURI(uri: String, store: PlaceStore = esStore) = store.findByMatchURI(uri)
  
  def searchByName(query: String, store: PlaceStore = esStore) = store.searchByName(query)

}

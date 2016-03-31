package models.place

import play.api.Logger

class MockPlaceStore extends PlaceStore {
  
  val mockIndex = scala.collection.mutable.HashMap.empty[String, Place]
  
  def totalPlaces() = {
    mockIndex.size
  }

  def insertOrUpdatePlace(place: Place) = {
    mockIndex.put(place.id, place)
  }
    
  def deletePlace(id: String) =
    mockIndex.remove(id)
    
  def findByURI(uri: String): Option[Place] = {
    val normalizedURI = GazetteerUtils.normalizeURI(uri)
    
    mockIndex.get(normalizedURI) match {
      case Some(hit) => 
        Some(hit)
        
      case None => // Might still be listed under alternative record URI
        mockIndex.values.find { place =>
          place.uris.contains(normalizedURI)
        }
    }
    
  }
  
  def findByPlaceOrMatchURIs(uris: Seq[String]) = {
    val normalized = uris.map(uri => GazetteerUtils.normalizeURI(uri)).toSet
    mockIndex.values.filter(place =>
      (place.uris ++ place.allMatches).exists(uri => normalized.contains(uri))).toSeq
  }
  
  def searchByName(name: String): Seq[Place] =
    mockIndex.values.toSeq
      .filter(place => {
        val names = place.names.keys.toSeq.map(_.name.toLowerCase)
        names.contains(name.toLowerCase)
      })
      
}
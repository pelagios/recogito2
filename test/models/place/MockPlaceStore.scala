package models.place

import play.api.Logger
import scala.concurrent.{ ExecutionContext, Future }

class MockPlaceStore extends PlaceStore {
  
  val mockIndex = scala.collection.mutable.HashMap.empty[String, Place]
  
  def totalPlaces()(implicit context: ExecutionContext) =
    Future(mockIndex.size.toLong)

  def insertOrUpdatePlace(place: Place)(implicit context: ExecutionContext) = 
    Future {
      mockIndex.put(place.id, place)
      true
    }
    
  def deletePlace(id: String)(implicit context: ExecutionContext) =
    Future {
      mockIndex.remove(id).isDefined
    }
    
  def findByURI(uri: String)(implicit context: ExecutionContext) =
    Future {
      val normalizedURI = GazetteerUtils.normalizeURI(uri)
      
      mockIndex.get(normalizedURI) match {
        case Some(hit) => 
          (Some(hit, 0l))
          
        case None => // Might still be listed under alternative record URI
          mockIndex.values.find { place =>
            place.uris.contains(normalizedURI)
          }.map((_, 0l))
      }
      
    }
  
  def findByPlaceOrMatchURIs(uris: Seq[String])(implicit context: ExecutionContext) =
    Future { 
      val normalized = uris.map(uri => GazetteerUtils.normalizeURI(uri)).toSet
        mockIndex.values.filter(place =>
          (place.uris ++ place.allMatches).exists(uri => normalized.contains(uri)))
        .toSeq
        .map((_, 0l))
    }
  
  def searchByName(name: String)(implicit context: ExecutionContext) =
    Future {
      mockIndex.values.toSeq
        .filter(place => {
          val names = place.names.keys.toSeq.map(_.name.toLowerCase)
          names.contains(name.toLowerCase)
        })
        .map((_, 0l))
    }
      
}
package models.place

import collection.JavaConverters._
import com.vividsolutions.jts.geom.Coordinate
import java.util.concurrent.ConcurrentHashMap
import play.api.Logger
import scala.concurrent.{ ExecutionContext, Future }
import models.Page

class MockPlaceStore extends PlaceStore {
  
  val mockIndex = new ConcurrentHashMap[String, Place]
  
  def totalPlaces()(implicit context: ExecutionContext) =
    Future {
      mockIndex.size
    }

  def listGazetteers()(implicit context: ExecutionContext) =
    throw new UnsupportedOperationException
  
  def insertOrUpdatePlace(place: Place)(implicit context: ExecutionContext) = 
    Future {
      mockIndex.put(place.id, place)
      (true, 0l)
    }
    
  def deletePlace(id: String)(implicit context: ExecutionContext) =
    Future {
      Option(mockIndex.remove(id)).isDefined
    }
    
  def findByURI(uri: String)(implicit context: ExecutionContext) =
    Future {
      val normalizedURI = GazetteerUtils.normalizeURI(uri)
      
      Option(mockIndex.get(normalizedURI)) match {
        case Some(hit) => 
          (Some(hit, 0l))
          
        case None => // Might still be listed under alternative record URI
          mockIndex.asScala.values.find { place =>
            place.uris.contains(normalizedURI)
          }.map((_, 0l))
      }
      
    }
  
  def findByPlaceOrMatchURIs(uris: Seq[String])(implicit context: ExecutionContext) =
    Future { 
      val normalized = uris.map(uri => GazetteerUtils.normalizeURI(uri)).toSet
        mockIndex.asScala.values.filter(place =>
          (place.uris ++ place.allMatches).exists(uri => normalized.contains(uri)))
        .toSeq
        .map((_, 0l))
    }
  
  def searchPlaces(name: String, offset: Int, limit: Int, sortFrom: Option[Coordinate])(implicit context: ExecutionContext) =
    Future {
      val results = mockIndex.asScala.values.toSeq
        .filter(place => {
          val names = place.names.keys.toSeq.map(_.name.toLowerCase)
          names.contains(name.toLowerCase)
        }).map((_, 0l))
      
      Page(0l, results.size, 0, limit, results.take(limit))
    }
      
}
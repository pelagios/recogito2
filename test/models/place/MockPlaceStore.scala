package models.place

import collection.JavaConverters._
import com.vividsolutions.jts.geom.Coordinate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import services.Page
import services.annotation.Annotation
import services.geotag.GeoTagStore
import play.api.Logger
import scala.concurrent.{ ExecutionContext, Future }

class MockPlaceStore extends PlaceStore with GeoTagStore {
  
  val mockIndex = new ConcurrentHashMap[String, Place]
  
  def totalPlaces()(implicit context: ExecutionContext) =
    Future {
      mockIndex.size
    }

  def listGazetteers()(implicit context: ExecutionContext) = ???
  
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
      val normalizedURI = GazetteerRecord.normalizeURI(uri)
      
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
      val normalized = uris.map(uri => GazetteerRecord.normalizeURI(uri)).toSet
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
  
  /** Unimplemented GeoTagStore methods **/
  def totalGeoTags()(implicit context: ExecutionContext) = ???
  def insertOrUpdateGeoTagsForAnnotation(annotation: Annotation)(implicit context: ExecutionContext) = ???
  def rewriteGeoTags(placesBeforeUpdate: Seq[Place], placesAfterUpdate: Seq[Place])(implicit context: ExecutionContext) = ???
  def deleteGeoTagsByAnnotation(annotationId: UUID)(implicit context: ExecutionContext) = ???
  def deleteGeoTagsByDocId(documentId: String)(implicit context: ExecutionContext) = ???
  def findGeoTagsByAnnotation(annotationId: UUID)(implicit context: ExecutionContext) = ???
  def listPlacesInDocument(docId: String, offset: Int = 0, limit: Int)(implicit context: ExecutionContext) = ???
  def searchPlacesInDocument(query: String, docId: String, offset: Int = 0, limit: Int)(implicit context: ExecutionContext) = ???
      
}
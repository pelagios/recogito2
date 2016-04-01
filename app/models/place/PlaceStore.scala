package models.place

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.source.Indexable
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import storage.ES

trait PlaceStore {

  /** Returns the total number of places in the store **/
  def totalPlaces(): Int
  
  /** Inserts a place **/
  def insertOrUpdatePlace(place: Place)
  
  /** Deletes the place with the specified ID **/
  def deletePlace(id: String)
  
  /** Retrieves a place by one of its gazetteer record URIs
    * 
    * Returns the place and a version number
    */
  def findByURI(uri: String): Future[Option[(Place, Long)]]
  
  /** Finds all places with a record URI or close/exactMatch URI that matches any of the supplied URIs **/
  def findByPlaceOrMatchURIs(uris: Seq[String]): Seq[Place]
  
  /** Searches places by name **/
  def searchByName(query: String): Seq[Place]
  
}

private[place] class ESPlaceStore extends PlaceStore {

  private val PLACE = "place"
  
  implicit object PlaceIndexable extends Indexable[Place] {
    override def json(p: Place): String = Json.stringify(Json.toJson(p))
  }

  implicit object PlaceHitAs extends HitAs[(Place, Long)] {
    override def as(hit: RichSearchHit): (Place, Long) =
      (Json.fromJson[Place](Json.parse(hit.sourceAsString)).get, hit.version)
  }
  
  def totalPlaces() = {
    // TODO implement
    0
  }

  def insertOrUpdatePlace(place: Place) = {
    ES.client execute { index into ES.IDX_RECOGITO / PLACE source place }
  }
  
  def deletePlace(id: String) = {
    
  }

  def findByURI(uri: String): Future[Option[(Place, Long)]] =
    ES.client execute {
      search in ES.IDX_RECOGITO / PLACE query nestedQuery("is_conflation_of").query(termQuery("is_conflation_of.uri" -> uri)) limit 10
    } map { response =>
      val placesAndVersions = response.as[(Place, Long)].toSeq 
      if (placesAndVersions.isEmpty) {
        None // No place with that URI
      } else { 
        if (placesAndVersions.size > 1)
          // This should never happen, unless something is wrong with the index!
          Logger.warn("Multiple places with URI " + uri) 
      
        Some(placesAndVersions.head)
      }
    }

  def findByPlaceOrMatchURIs(uris: Seq[String]): Seq[Place] = {
    Seq.empty[Place]
  }

  def searchByName(query: String): Seq[Place] = {
    // TODO implement
    Seq.empty[Place]
  }

}
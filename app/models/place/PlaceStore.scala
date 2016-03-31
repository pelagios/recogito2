package models.place

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.source.Indexable
import play.api.libs.json.Json

trait PlaceStore {

  /** Returns the total number of places in the store **/
  def totalPlaces(): Int
  
  /** Inserts a place **/
  def insertOrUpdatePlace(place: Place)
  
  /** Retrieves a place by one of its gazetteer record URIs **/
  def findByURI(uri: String): Option[Place]
  
  /** Finds all places with a record URI or close/exactMatch URI that matches any of the supplied URIs **/
  def findByPlaceOrMatchURIs(uris: Seq[String]): Seq[Place]
  
  /** Searches places by name **/
  def searchByName(query: String): Seq[Place]
  
}

private[place] class ESPlaceStore extends PlaceStore {

  private val PLACE = "place"
  
  def totalPlaces() = {
    // TODO implement
    0
  }

  def insertOrUpdatePlace(place: Place) = {
    // TODO implement
    // ES.client execute { index into ES.IDX_RECOGITO / PLACE source place }
  }

  def findByURI(uri: String): Option[Place] = {
    // TODO implement
    // ES.client execute {
    //  search in ES.IDX_RECOGITO / PLACE query nestedQuery("is_conflation_of").query(termQuery("is_conflation_of.uri" -> uri)) limit 1
    // } map(_.as[Place].toSeq)
    Option.empty[Place]
  }

  def findByPlaceOrMatchURIs(uris: Seq[String]): Seq[Place] = {
    Seq.empty[Place]
  }

  def searchByName(query: String): Seq[Place] = {
    // TODO implement
    Seq.empty[Place]
  }

}

private[place] object ESPlaceStore {
  
  implicit object PlaceIndexable extends Indexable[Place] {
    override def json(p: Place): String = Json.stringify(Json.toJson(p))
  }

  implicit object PlaceHitAs extends HitAs[Place] {
    override def as(hit: RichSearchHit): Place =
      Json.fromJson[Place](Json.parse(hit.sourceAsString)).get
  }
  
}

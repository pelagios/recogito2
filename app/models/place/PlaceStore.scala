package models.place

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.source.Indexable
import play.api.libs.json.Json

trait PlaceStore {

  def totalPlaces(): Int
  
  def insertPlace(place: Place)
  
  def deletePlace(id: String)
  
  def findByURI(uri: String): Option[Place]
  
  def findByMatchURI(uri: String): Seq[Place] 
  
  def searchByName(query: String): Seq[Place]
  
}

private[place] class ESPlaceStore extends PlaceStore {

  private val PLACE = "place"
  
  def totalPlaces() = {
    // TODO implement
    0
  }

  def insertPlace(place: Place) = {
    // TODO implement
    // ES.client execute { index into ES.IDX_RECOGITO / PLACE source place }
  }
  
  def deletePlace(id: String) = {
    
  }

  def findByURI(uri: String): Option[Place] = {
    // TODO implement
    // ES.client execute {
    //  search in ES.IDX_RECOGITO / PLACE query nestedQuery("is_conflation_of").query(termQuery("is_conflation_of.uri" -> uri)) limit 1
    // } map(_.as[Place].toSeq)
    Option.empty[Place]
  }

  def findByMatchURI(uri: String): Seq[Place] = {
    // TODO implement - make sure exactmatches are queried as well
    // ES.client execute {
    //  search in ES.IDX_RECOGITO / PLACE query nestedQuery("is_conflation_of").query(termQuery("is_conflation_of.close_matches" -> uri)) limit 1
    // } map(_.as[Place].toSeq)
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

package models.place

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.source.Indexable
import models.Page
import models.geotag.GeoTagServiceLike
import org.elasticsearch.search.sort.SortOrder
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import scala.language.postfixOps
import storage.ES

trait PlaceStore {

  /** Returns the total number of places in the store **/
  def totalPlaces()(implicit context: ExecutionContext): Future[Long]
  
  /** Inserts a place
    *
    * Returns true if insert was successful  
    */
  def insertOrUpdatePlace(place: Place)(implicit context: ExecutionContext): Future[(Boolean, Long)]
  
  /** Deletes the place with the specified ID
    *
    * Returns true if the delete was successful
    */
  def deletePlace(id: String)(implicit context: ExecutionContext): Future[Boolean]
  
  /** Retrieves a place by one of its gazetteer record URIs
    * 
    * Returns the place and a version number
    */
  def findByURI(uri: String)(implicit context: ExecutionContext): Future[Option[(Place, Long)]]
  
  /** Finds all places with a record URI or close/exactMatch URI that matches any of the supplied URIs **/
  def findByPlaceOrMatchURIs(uris: Seq[String])(implicit context: ExecutionContext): Future[Seq[(Place, Long)]]

  /** Finds all places in a document **/ 
  def findPlacesInDocument(docId: String)(implicit context: ExecutionContext): Future[Seq[(Place, Long)]]
  
  /** Place search **/
  def searchPlaces(query: String, limit: Int = 20)(implicit context: ExecutionContext): Future[Page[(Place, Long)]]
  
  /** Search places in a document **/
  def searchPlacesInDocument(query: String, docId: String, limit: Int = 20)(implicit context: ExecutionContext): Future[Page[(Place, Long)]]
  
}

private[models] class ESPlaceStore extends PlaceStore with GeoTagServiceLike {

  private[place] val PLACE = "place"
  
  implicit object PlaceIndexable extends Indexable[Place] {
    override def json(p: Place): String = Json.stringify(Json.toJson(p))
  }

  implicit object PlaceHitAs extends HitAs[(Place, Long)] {
    override def as(hit: RichSearchHit): (Place, Long) =
      (Json.fromJson[Place](Json.parse(hit.sourceAsString)).get, hit.version)
  }
  
  def totalPlaces()(implicit context: ExecutionContext): Future[Long] =
    ES.client execute {
      search in ES.IDX_RECOGITO -> PLACE limit 0
    } map { _.getHits.getTotalHits }
    
  def insertOrUpdatePlace(place: Place)(implicit context: ExecutionContext): Future[(Boolean, Long)] =
    ES.client execute { 
      update id place.id in ES.IDX_RECOGITO / PLACE source place docAsUpsert 
    } map { r =>
      (true, r.getVersion)
    } recover { case t: Throwable =>
      Logger.error("Error indexing place " + place.id + ": " + t.getMessage)
      t.printStackTrace
      (false, -1l)
    }
    
  def deletePlace(id: String)(implicit context: ExecutionContext): Future[Boolean] =
    ES.client execute { 
      delete id id from ES.IDX_RECOGITO / PLACE
    } map { response =>
      response.isFound
    }    
 
  def findByURI(uri: String)(implicit context: ExecutionContext): Future[Option[(Place, Long)]] =
    ES.client execute {
      search in ES.IDX_RECOGITO -> PLACE query nestedQuery("is_conflation_of").query(termQuery("is_conflation_of.uri" -> uri)) limit 10
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

  def findByPlaceOrMatchURIs(uris: Seq[String])(implicit context: ExecutionContext): Future[Seq[(Place, Long)]] = {
    ES.client execute {
      search in ES.IDX_RECOGITO / PLACE query { 
        nestedQuery("is_conflation_of").query {
          bool {
            should { 
              uris.map(uri => termQuery("is_conflation_of.uri" -> uri)) ++
              uris.map(uri => termQuery("is_conflation_of.close_matches" -> uri)) ++
              uris.map(uri => termQuery("is_conflation_of.exact_matches" -> uri))
            }
          }
        } 
      } limit 100
    } map { _.as[(Place, Long)].toSeq 
    }
  }
  
  def findPlacesInDocument(docId: String)(implicit context: ExecutionContext) =
    ES.client execute {
      search in ES.IDX_RECOGITO / "place" query {
        hasChildQuery(GEOTAG).query {
          termQuery("document_id", docId)
        }
      }
    } map { _.as[(Place, Long)] }

  def searchPlaces(q: String, l: Int)(implicit context: ExecutionContext): Future[Page[(Place, Long)]] =
    ES.client execute {
      search in ES.IDX_RECOGITO / PLACE query {
        nestedQuery("is_conflation_of").query {
          bool {
            should (
              // Search inside record titles...
              matchPhraseQuery("is_conflation_of.title.raw", q).boost(5.0),
              matchPhraseQuery("is_conflation_of.title", q),
             
              // ...names...
              nestedQuery("is_conflation_of.names").query {
                matchPhraseQuery("is_conflation_of.names.name.raw", q).boost(5.0)
              },
             
              nestedQuery("is_conflation_of.names").query {
                matchQuery("is_conflation_of.names.name", q)
              },
             
              // ...and descriptions (with lower boost)
              nestedQuery("is_conflation_of.descriptions").query {
                matchQuery("is_conflation_of.descriptions.description", q)
              }.boost(0.2)
            )
          }
        }
      } limit l
    } map { response =>
      val places = response.as[(Place, Long)].toSeq 
      Page(response.getTook.getMillis, response.getHits.getTotalHits, 0, l, places)
    }
    
  def searchPlacesInDocument(q: String, docId: String, l: Int)(implicit context: ExecutionContext) =
    ES.client execute {
      search in ES.IDX_RECOGITO / "place" query {
        bool {
          
          must(
            nestedQuery("is_conflation_of").query {
              bool {
                should (
                  // Search inside record titles...
                  matchPhraseQuery("is_conflation_of.title.raw", q).boost(5.0),
                  matchPhraseQuery("is_conflation_of.title", q),
                 
                  // ...names...
                  nestedQuery("is_conflation_of.names").query {
                    matchPhraseQuery("is_conflation_of.names.name.raw", q).boost(5.0)
                  },
                 
                  nestedQuery("is_conflation_of.names").query {
                    matchQuery("is_conflation_of.names.name", q)
                  },
                 
                  // ...and descriptions (with lower boost)
                  nestedQuery("is_conflation_of.descriptions").query {
                    matchQuery("is_conflation_of.descriptions.description", q)
                  }.boost(0.2)
                )
              }
            },  
            
            hasChildQuery(GEOTAG).query {
              termQuery("document_id", docId)
            }
          )
        }
      } limit l
    } map { response =>
      val places = response.as[(Place, Long)].toSeq 
      Page(response.getTook.getMillis, response.getHits.getTotalHits, 0, l, places)
    }  
    
}
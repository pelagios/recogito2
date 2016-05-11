package models.geotag

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.source.Indexable
import java.util.UUID
import models.annotation.{ Annotation, AnnotationBody }
import models.place.Place
import models.place.PlaceService
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import storage.ES

/** A trait for adding GeoTag read/write functionality to a service **/
object GeoTagServiceLike {
 
  private val GEOTAG = "geotag"
  
  implicit object GeoTagIndexable extends Indexable[GeoTag] {
    override def json(link: GeoTag): String = Json.stringify(Json.toJson(link))
  }

  implicit object GeoTagHitAs extends HitAs[(String, GeoTag)] {
    override def as(hit: RichSearchHit): (String, GeoTag) =
      (hit.id, Json.fromJson[GeoTag](Json.parse(hit.sourceAsString)).get)
  }
  
  private def buildGeoTags(annotation: Annotation)(implicit context: ExecutionContext): Future[Seq[GeoTag]] = {

    def createGeoTag(annotation: Annotation, placeBody: AnnotationBody, placeId: String) =
      GeoTag(
        placeId,
        annotation.annotationId,
        annotation.annotates.document,
        annotation.annotates.filepart,
        placeBody.uri.get)
    
    // These are all place bodies that have a URI set
    val placeBodies = annotation.bodies.filter(body => body.hasType == AnnotationBody.PLACE && body.uri.isDefined)
    
    if (placeBodies.isEmpty)
      Future.successful(Seq.empty[GeoTag])
      
    else
      Future.sequence(placeBodies.map(body => 
        PlaceService.findByURI(body.uri.get).map(_ match {
          case Some((place, version)) => createGeoTag(annotation, body, place.id)
            
          case None =>
            // Annotation links to a place not found in the gazetteer - can never happen unless something's broken
            throw new Exception("Annotation " + annotation.annotationId + " links to " + body.uri.get + " - but not found in gazetteer")
        })
      ))
  }
  
  /** Helper method that retrieves geotags along with their internal _id field **/
  private def findGeoTagsByAnnotationWithId(annotationId: String)(implicit context: ExecutionContext): Future[Seq[(String, GeoTag)]] =
    ES.client execute {
      search in ES.IDX_RECOGITO / GEOTAG query {
        termQuery("annotation_id", annotationId)
      }
    } map { _.as[(String, GeoTag)].toSeq }
  
  /** Retrieves the links for a specific annotation ID **/
  def findGeoTagsByAnnotation(annotationId: UUID)(implicit context: ExecutionContext): Future[Seq[GeoTag]] =
    findGeoTagsByAnnotationWithId(annotationId.toString).map(_.map(_._2))   
  
  /** Deletes the links for a specific annotation ID.
    *
    * Unfortunately, ElasticSearch doesn't support delete-by-query directly,
    * so this is a two-step-process.  
    */
  def deleteGeoTagsByAnnotation(annotationId: UUID)(implicit context: ExecutionContext): Future[Boolean] =
    findGeoTagsByAnnotationWithId(annotationId.toString).flatMap { idsAndLinks => 
      if (idsAndLinks.size > 0) {
        ES.client execute {
          bulk ( idsAndLinks.map { case (linkId, _) => delete id linkId from ES.IDX_RECOGITO / GEOTAG } )
        } map { 
          !_.hasFailures 
        } recover { case t: Throwable =>
          t.printStackTrace()
          false
        }
      } else {
        // Nothing to delete
        Future.successful(true)
      }
    }
  
  /** Inserts or updates place links for an annotation **/
  def insertOrUpdateGeoTagsForAnnotation(annotation: Annotation)(implicit context: ExecutionContext): Future[Boolean] = {
    
    def insertGeoTags(tags: Seq[GeoTag]): Future[Boolean] = {      
      ES.client execute {
        bulk ( tags.map(tag => index into ES.IDX_RECOGITO / GEOTAG source tag parent tag.placeId) )
      } map {
        !_.hasFailures
      } recover { case t: Throwable => 
        t.printStackTrace()
        false
      }
    }
    
    // Since checking for changes would require an extra request cycle (and application-side comparison) anyway,
    // we just delete existing links and create the new ones
    for {
      tagsToInsert <- buildGeoTags(annotation)
      deleteSuccess <- deleteGeoTagsByAnnotation(annotation.annotationId)
      insertSuccess <- if (deleteSuccess && tagsToInsert.size > 0) insertGeoTags(tagsToInsert) else Future.successful(false) 
    } yield insertSuccess
  }
  
  // TODO get rid of this! Clean up mix between PlaceService, PlaceStore, PlaceLinkService
  implicit object PlaceHitAs extends HitAs[Place] {
    override def as(hit: RichSearchHit): Place =
      Json.fromJson[Place](Json.parse(hit.sourceAsString)).get
  }
  
  def getPlacesInDocument(docId: String)(implicit context: ExecutionContext) =
    ES.client execute {
      search in ES.IDX_RECOGITO / "place" query {
        hasChildQuery(GEOTAG).query {
          termQuery("document_id", docId)
        }
      }
    } map { _.as[Place] }  
  
  def searchPlacesInDocument(q: String, documentId: String)(implicit context: ExecutionContext) =
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
              termQuery("document_id", documentId)
            }
          )
        }
      }
    } map { _.as[Place] }  
    
  def totalGeoTags()(implicit context: ExecutionContext): Future[Long] =
    ES.client execute {
      search in ES.IDX_RECOGITO -> GEOTAG limit 0
    } map { _.getHits.getTotalHits }
  
}
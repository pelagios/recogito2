package models.place

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.source.Indexable
import java.util.UUID
import models.annotation.Annotation
import models.annotation.AnnotationBody
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import storage.ES

object PlaceLinkService {
 
  private val PLACE_LINK = "place_link"
  
  implicit object PlaceLinkIndexable extends Indexable[PlaceLink] {
    override def json(link: PlaceLink): String = Json.stringify(Json.toJson(link))
  }

  implicit object PlaceLinkHitAs extends HitAs[(String, PlaceLink)] {
    override def as(hit: RichSearchHit): (String, PlaceLink) =
      (hit.id, Json.fromJson[PlaceLink](Json.parse(hit.sourceAsString)).get)
  }
  
  private def buildPlaceLinks(annotation: Annotation)(implicit context: ExecutionContext): Future[Seq[PlaceLink]] = {

    def createPlaceLink(annotation: Annotation, placeBody: AnnotationBody, placeId: String) =
      PlaceLink(
        placeId,
        annotation.annotationId,
        annotation.annotates.document,
        annotation.annotates.filepart,
        placeBody.uri.get)
    
    // These are all place bodies that have a URI set
    val placeBodies = annotation.bodies.filter(body => body.hasType == AnnotationBody.PLACE && body.uri.isDefined)
    
    if (placeBodies.isEmpty)
      Future.successful(Seq.empty[PlaceLink])
      
    else
      Future.sequence(placeBodies.map(body => 
        PlaceService.findByURI(body.uri.get).map(_ match {
          case Some((place, version)) => createPlaceLink(annotation, body, place.id)
            
          case None =>
            // Annotation links to a place not found in the gazetteer - can never happen unless something's broken
            throw new Exception("Annotation " + annotation.annotationId + " links to " + body.uri.get + " - but not found in gazetteer")
        })
      ))
  }
  
  /** Helper method that retrieves place links along with their internal _id field **/
  private def findByAnnotationIdWith_Id(annotationId: String)(implicit context: ExecutionContext): Future[Seq[(String, PlaceLink)]] =
    ES.client execute {
      search in ES.IDX_RECOGITO / PLACE_LINK query {
        termQuery("annotation_id", annotationId)
      }
    } map { _.as[(String, PlaceLink)].toSeq }
  
  /** Retrieves the links for a specific annotation ID **/
  def findByAnnotationId(annotationId: UUID)(implicit context: ExecutionContext): Future[Seq[PlaceLink]] =
    findByAnnotationIdWith_Id(annotationId.toString).map(_.map(_._2))   
  
  /** Deletes the links for a specific annotation ID.
    *
    * Unfortunately, ElasticSearch doesn't support delete-by-query directly,
    * so this is a two-step-process.  
    */
  def deleteByAnnotationId(annotationId: UUID)(implicit context: ExecutionContext): Future[Boolean] =
    findByAnnotationIdWith_Id(annotationId.toString).flatMap { idsAndLinks => 
      if (idsAndLinks.size > 0) {
        ES.client execute {
          bulk ( idsAndLinks.map { case (linkId, _) => delete id linkId from ES.IDX_RECOGITO / PLACE_LINK } )
        } map { 
          !_.hasFailures() 
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
  def insertOrUpdatePlaceLinksForAnnotation(annotation: Annotation)(implicit context: ExecutionContext): Future[Boolean] = {
    
    def insertPlaceLinks(links: Seq[PlaceLink]): Future[Boolean] = {      
      ES.client execute {
        bulk ( links.map(link => index into ES.IDX_RECOGITO / PLACE_LINK source link parent link.placeId) )
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
      linksToInsert <- buildPlaceLinks(annotation)
      deleteSuccess <- deleteByAnnotationId(annotation.annotationId)
      insertSuccess <- if (deleteSuccess) insertPlaceLinks(linksToInsert) else Future.successful(false) 
    } yield insertSuccess
  }
  
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
            
            hasChildQuery("place_link").query {
              termQuery("document_id", documentId)
            }
          )
        }

      }
    }  
    
  def totalPlaceLinks()(implicit context: ExecutionContext): Future[Long] =
    ES.client execute {
      search in ES.IDX_RECOGITO -> PLACE_LINK limit 0
    } map { _.getHits.getTotalHits }
  
}
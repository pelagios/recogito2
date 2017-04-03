package models.geotag

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit, RichSearchResponse }
import com.sksamuel.elastic4s.source.Indexable
import java.util.UUID
import models.Page
import models.annotation.{ Annotation, AnnotationBody }
import models.place.{ ESPlaceStore, Place, PlaceStore }
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import scala.language.{ postfixOps, reflectiveCalls }
import storage.{ ES, HasES }

trait GeoTagStore extends PlaceStore {

  /** Returns the total number of geotags in the store **/
  def totalGeoTags()(implicit context: ExecutionContext): Future[Long]

  /** Inserts or updates geotags for an annotation **/
  def insertOrUpdateGeoTagsForAnnotation(annotation: Annotation)(implicit context: ExecutionContext): Future[Boolean]

  /** Re-writes GeoTags after an update to the place store **/
  def rewriteGeoTags(placesBeforeUpdate: Seq[Place], placesAfterUpdate: Seq[Place])(implicit context: ExecutionContext): Future[Boolean]

  /** Deletes the geotags for a specific annotation ID **/
  def deleteGeoTagsByAnnotation(annotationId: UUID)(implicit context: ExecutionContext): Future[Boolean]

  /** Deletes the geotags for a specific document **/
  def deleteGeoTagsByDocId(documentId: String)(implicit context: ExecutionContext): Future[Boolean]

  /** Retrieves the links for a specific annotation ID **/
  def findGeoTagsByAnnotation(annotationId: UUID)(implicit context: ExecutionContext): Future[Seq[GeoTag]]

  /** Lists all places in a document **/
  def listPlacesInDocument(docId: String, offset: Int = 0, limit: Int = ES.MAX_SIZE)(implicit context: ExecutionContext): Future[Page[(Place, Long)]]

  /** Search places in a document **/
  def searchPlacesInDocument(query: String, docId: String, offset: Int = 0, limit: Int = ES.MAX_SIZE)(implicit context: ExecutionContext): Future[Page[(Place, Long)]]

}

private[models] trait ESGeoTagStore extends ESPlaceStore with GeoTagStore { self: HasES =>

  implicit object GeoTagIndexable extends Indexable[GeoTag] {
    override def json(link: GeoTag): String = Json.stringify(Json.toJson(link))
  }

  implicit object GeoTagHitAs extends HitAs[(GeoTag, String, String)] {
    override def as(hit: RichSearchHit): (GeoTag, String, String) = {
      val id = hit.id
      val parent = hit.field("_parent").value[String]
      val geoTag = Json.fromJson[GeoTag](Json.parse(hit.sourceAsString)).get
      (geoTag, id, parent)
    }
  }

  override def totalGeoTags()(implicit context: ExecutionContext): Future[Long] =
    es.client execute {
      search in ES.RECOGITO -> ES.GEOTAG limit 0
    } map { _.getHits.getTotalHits }

  /** Helper used by insertOrUpdate method to build the geotags from the annotation bodies **/
  private def buildGeoTags(annotation: Annotation)(implicit context: ExecutionContext): Future[Seq[(GeoTag, String)]] = {

    def getToponyms(annotation: Annotation): Seq[String] =
      annotation.bodies
        .withFilter(b => b.hasType == AnnotationBody.QUOTE || b.hasType == AnnotationBody.TRANSCRIPTION)
        .flatMap(_.value)

    def createGeoTag(annotation: Annotation, placeBody: AnnotationBody) =
      GeoTag(
        annotation.annotationId,
        annotation.annotates.documentId,
        annotation.annotates.filepartId,
        placeBody.uri.get,
        getToponyms(annotation),
        annotation.contributors,
        annotation.lastModifiedBy,
        annotation.lastModifiedAt)

    // These are all place bodies that have a URI set
    val placeBodies = annotation.bodies.filter(body => body.hasType == AnnotationBody.PLACE && body.uri.isDefined)

    if (placeBodies.isEmpty)
      Future.successful(Seq.empty[(GeoTag, String)])

    else
      Future.sequence(placeBodies.map(body =>
        findByURI(body.uri.get).map {
          case Some((place, version)) => (createGeoTag(annotation, body), place.id)

          case None =>
            // Annotation links to a place not found in the gazetteer - can never happen unless something's broken
            throw new Exception("Annotation " + annotation.annotationId + " links to " + body.uri.get + " - but not found in gazetteer")
        }
      ))
  }

  override def insertOrUpdateGeoTagsForAnnotation(annotation: Annotation)(implicit context: ExecutionContext): Future[Boolean] = {

    def insertGeoTags(tags: Seq[(GeoTag, String)]): Future[Boolean] = {
      es.client execute {
        bulk ( tags.map(tag => index into ES.RECOGITO / ES.GEOTAG source tag._1 parent tag._2) )
      } map { response =>
        if (response.hasFailures)
          Logger.error("Failures while inserting geotags: " + response.failureMessage)
        !response.hasFailures
      } recover { case t: Throwable =>
        t.printStackTrace()
        false
      }
    }

    // Since checking for changes would require an extra request cycle (and application-side comparison) anyway,
    // we just delete existing links and create the new ones
    val f = for {
      tagsToInsert <- buildGeoTags(annotation)
      deleteSuccess <- deleteGeoTagsByAnnotation(annotation.annotationId)
      insertSuccess <- if (deleteSuccess && tagsToInsert.nonEmpty) insertGeoTags(tagsToInsert) else Future.successful(deleteSuccess)
    } yield insertSuccess

    f.recover { case t: Throwable =>
      t.printStackTrace()
      false
    }
  }

  def rewriteGeoTags(placesBeforeUpdate: Seq[Place], placesAfterUpdate: Seq[Place])(implicit context: ExecutionContext): Future[Boolean] = {
    
    def fetchNextBatch(scrollId: String): Future[RichSearchResponse] =
      es.client execute {
        search scroll scrollId keepAlive "5m"
      }

    def rewriteOne(tag: GeoTag, id: String, parent: String) = {
      val oldParent = placesBeforeUpdate.find(_.uris.contains(tag.gazetteerUri)).get
      val newParent = placesAfterUpdate.find(_.uris.contains(tag.gazetteerUri)).get
      if (oldParent.id != newParent.id) {
        Logger.debug("Rewriting geotag reference: " + tag.gazetteerUri)
        
        /*
        Logger.debug("  from "  + oldParent.id + " to " + newParent.id)
        Logger.debug("  was " + placesBeforeUpdate.size + " places before and " + placesAfterUpdate.size + " after update")
        
        Logger.debug("  places before: " + placesBeforeUpdate.map(_.uris.mkString("\n")))
        Logger.debug("  places after: " + placesAfterUpdate.map(_.uris.mkString("\n")))
        */
        
        es.client execute {
          bulk (
            delete id id from ES.RECOGITO / ES.GEOTAG parent parent, 
            index into ES.RECOGITO / ES.GEOTAG source tag parent newParent.id
          )
        } map { !_.hasFailures }
      } else {
        // Logger.info("no rewrite")
        Future.successful(true)
      }
    }
    
    def rewriteBatch(response: RichSearchResponse, cursor: Long = 0l): Future[Boolean] = {
       val total = response.totalHits
       val tags = response.as[(GeoTag, String, String)].toSeq
       
       if (tags.isEmpty) {
         Future.successful(true)
       } else {
         val fSuccesses = Future.sequence(tags.map { case (tag, id, parent) => rewriteOne(tag, id, parent) })
         fSuccesses.flatMap { successes =>
           val success = !successes.exists(_ == false)
           val rewrittenTags = cursor + tags.size
           if (rewrittenTags < total)
             fetchNextBatch(response.scrollId).flatMap { response =>
               rewriteBatch(response, rewrittenTags).map { _ && success }
             }
           else
             Future.successful(success)
         }
       }
    }

    if (placesBeforeUpdate.size > 0) {
      es.client execute {
        search in ES.RECOGITO / ES.GEOTAG query {
          bool {
            should (
              placesBeforeUpdate.map { place => hasParentQuery(ES.PLACE).query { termQuery("id", place.id) } }
            )
          }
        } limit 50 scroll "5m"
      } flatMap {
        rewriteBatch(_)
      }
    } else {
      // No need to update any GeoTags if no existing places were affected by the import
      Future.successful(true)
    }
  }

  /** Helper to bulk-delete a list of GeoTags **/
  private def bulkDelete(idsAndParents: Seq[(String, String)])(implicit context: ExecutionContext): Future[Boolean] =
    if (idsAndParents.isEmpty) {
      // Nothing to delete
      Future.successful(true)
    } else {
      es.client execute {
        bulk ( idsAndParents.map { case (tagId, parentId) => delete id tagId from ES.RECOGITO / ES.GEOTAG parent parentId } )
      } map { response =>
        if (response.hasFailures)
          Logger.error("Failures while deleting geotags: " + response.failureMessage)
        !response.hasFailures
      } recover { case t: Throwable =>
        t.printStackTrace()
        false
      }
    }

  /** Helper method that retrieves geotags for an annotation along with their internal _id field **/
  private def findGeoTagsByAnnotationWithIdAndParent(annotationId: String)(implicit context: ExecutionContext): Future[Seq[(GeoTag, String, String)]] =
    es.client execute {
      search in ES.RECOGITO / ES.GEOTAG query {
        termQuery("annotation_id", annotationId)
      }
    } map { _.as[(GeoTag, String, String)].toSeq }

  /** Unfortunately, ElasticSearch doesn't support delete-by-query directly, so this is a two-step-process **/
  override def deleteGeoTagsByAnnotation(annotationId: UUID)(implicit context: ExecutionContext): Future[Boolean] =
    findGeoTagsByAnnotationWithIdAndParent(annotationId.toString).flatMap { tags =>
      bulkDelete(tags.map(t => (t._2, t._3)))
    }

  override def deleteGeoTagsByDocId(documentId: String)(implicit context: ExecutionContext): Future[Boolean] = {

    def findIdsForDoc(documentId: String) =
      es.client execute {
        search in ES.RECOGITO / ES.GEOTAG query {
          termQuery("document_id" -> documentId)
        }
      } map { _.as[(GeoTag, String, String)].map(t => (t._2, t._3)) }

    findIdsForDoc(documentId).flatMap(bulkDelete(_))
  }

  override def findGeoTagsByAnnotation(annotationId: UUID)(implicit context: ExecutionContext): Future[Seq[GeoTag]] =
    findGeoTagsByAnnotationWithIdAndParent(annotationId.toString).map(_.map(_._1))

  override def listPlacesInDocument(docId: String, offset: Int, limit: Int)(implicit context: ExecutionContext) =
    es.client execute {
      search in ES.RECOGITO / ES.PLACE query {
        hasChildQuery(ES.GEOTAG).query {
          termQuery("document_id", docId)
        }
      } start offset limit limit
    } map { response =>
      val places = response.as[(Place, Long)]
      Page(response.getTook.getMillis, response.getHits.getTotalHits, offset, limit, places)
    }

  override def searchPlacesInDocument(q: String, docId: String, offset: Int, limit: Int)(implicit context: ExecutionContext) =
    es.client execute {
      search in ES.RECOGITO / ES.PLACE query {
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

            hasChildQuery(ES.GEOTAG).query {
              termQuery("document_id", docId)
            }
          )
        }
      } start offset limit limit
    } map { response =>
      val places = response.as[(Place, Long)].toSeq
      Page(response.getTook.getMillis, response.getHits.getTotalHits, offset, limit, places)
    }

}

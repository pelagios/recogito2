package models.place

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit, SearchType }
import com.sksamuel.elastic4s.source.Indexable
import com.vividsolutions.jts.geom.Coordinate
import models.Page
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.sort.SortOrder
import play.api.Logger
import play.api.libs.json.Json
import scala.collection.JavaConverters._
import scala.concurrent.{ Future, ExecutionContext }
import scala.language.postfixOps
import storage.ES

trait PlaceStore {

  /** Returns the total number of places in the store **/
  def totalPlaces()(implicit context: ExecutionContext): Future[Long]

  /** Lists the names of stored gazetteers **/
  def listGazetteers()(implicit context: ExecutionContext): Future[Seq[String]]

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

  /** Place search **/
  def searchPlaces(query: String, offset: Int = 0, limit: Int = 20, sortFrom: Option[Coordinate] = None)(implicit context: ExecutionContext): Future[Page[(Place, Long)]]

}

private[models] trait ESPlaceStore extends PlaceStore with PlaceImporter {

  private val PLACE = "place"

  private def MAX_RETRIES = 10 // Max number of import retires in case of failure

  implicit object PlaceIndexable extends Indexable[Place] {
    override def json(p: Place): String = Json.stringify(Json.toJson(p))
  }

  implicit object PlaceHitAs extends HitAs[(Place, Long)] {
    override def as(hit: RichSearchHit): (Place, Long) =
      (Json.fromJson[Place](Json.parse(hit.sourceAsString)).get, hit.version)
  }

  override def totalPlaces()(implicit context: ExecutionContext): Future[Long] =
    ES.client execute {
      search in ES.IDX_RECOGITO -> PLACE limit 0
    } map { _.getHits.getTotalHits }

  override def listGazetteers()(implicit context: ExecutionContext): Future[Seq[String]] =
    ES.client execute {
      search in ES.IDX_RECOGITO / PLACE aggs (
        aggregation nested("by_source_gazetteer") path "is_conflation_of" aggs (
          aggregation terms "source_gazetteer" field "is_conflation_of.source_gazetteer" size Int.MaxValue
        )
      ) limit 0
    } map { response =>
      response.getAggregations.get("by_source_gazetteer").asInstanceOf[Nested]
              .getAggregations.get("source_gazetteer").asInstanceOf[Terms]
              .getBuckets.asScala
              .map(_.getKey)
    }

  override def insertOrUpdatePlace(place: Place)(implicit context: ExecutionContext): Future[(Boolean, Long)] =
    ES.client execute {
      update id place.id in ES.IDX_RECOGITO / PLACE source place docAsUpsert
    } map { r =>
      (true, r.getVersion)
    } recover { case t: Throwable =>
      Logger.error("Error indexing place " + place.id + ": " + t.getMessage)
      t.printStackTrace
      (false, -1l)
    }

  override def deletePlace(id: String)(implicit context: ExecutionContext): Future[Boolean] =
    ES.client execute {
      delete id id from ES.IDX_RECOGITO / PLACE
    } map { response =>
      response.isFound
    }

  override def findByURI(uri: String)(implicit context: ExecutionContext): Future[Option[(Place, Long)]] =
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

  override def findByPlaceOrMatchURIs(uris: Seq[String])(implicit context: ExecutionContext): Future[Seq[(Place, Long)]] =
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
    } map { _.as[(Place, Long)].toSeq }

  override def searchPlaces(q: String, offset: Int, limit: Int, sortFrom: Option[Coordinate])(implicit context: ExecutionContext): Future[Page[(Place, Long)]] = {
    ES.client execute {
      val query = search in ES.IDX_RECOGITO / PLACE query {
        bool {
          should (
            // Treat as standard query string query first...
            queryStringQuery(q).defaultOperator("AND"),

            // ...and then look for exact matches in specific fields
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
                    matchPhraseQuery("is_conflation_of.names.name", q)
                  },

                  // ...and descriptions (with lower boost)
                  nestedQuery("is_conflation_of.descriptions").query {
                    matchQuery("is_conflation_of.descriptions.description", q).operator("AND")
                  }.boost(0.2)
                )
              }
            }
          )
        }
      } start offset limit limit
      
      sortFrom match {
        case Some(coord) => query sort ( geoSort("representative_point") point(coord.y, coord.x) order SortOrder.ASC )
        case None        => query
      }
    } map { response =>
      val places = response.as[(Place, Long)].toSeq
      Page(response.getTook.getMillis, response.getHits.getTotalHits, 0, limit, places)
    } 
  }

  private def scrollByGazetteer(gazetteer: String, fn: Place => Future[Boolean])(implicit context: ExecutionContext) = {

    // Applies the fn to a seq of places, in sequence, without blocking, handling retries
    def applySequential(places: Seq[Place], retries: Int = MAX_RETRIES): Future[Seq[Place]] =
      places.foldLeft(Future.successful(Seq.empty[Place])) { case (future, place) =>
        future.flatMap { failedPlaces =>
          fn(place).map { success =>
            if (success) failedPlaces else failedPlaces :+ place
          }
        }
      } flatMap { failed =>
        if (failed.size > 0 && retries > 0) {
          Logger.warn(failed.size + " gazetteer records failed to process - retrying")
          applySequential(failed, retries - 1)
        } else {
          Logger.info("Successfully processed " + (places.size - failed.size) + " records")
          if (failed.size > 0)
            Logger.error(failed.size + " gazetteer records failed without recovery")
          else
            Logger.info("None failed")

          Future.successful(failed)
        }
      }

    // Fetch one scroll batch, processes the results and run next batch
    def processOneBatch(scrollId: String, cursor: Long = 0l): Future[Unit] =
      ES.client execute {
        search scroll scrollId keepAlive "1m"
      } flatMap { response =>
        applySequential(response.as[(Place, Long)].map(_._1)).map { _ =>
          val processedRecords = cursor + response.getHits.getHits.size
          if (processedRecords < response.getHits.getTotalHits)
            processOneBatch(response.getScrollId, processedRecords)
        }
      }

    // Initial search request
    ES.client execute {
      search in ES.IDX_RECOGITO / PLACE query
        nestedQuery("is_conflation_of").query(termQuery("is_conflation_of.source_gazetteer" -> gazetteer)) searchType SearchType.Scan scroll "1m"
    } map { response =>
      processOneBatch(response.getScrollId)
    }

  }

  def deleteByGazetteer(gazetteer: String)(implicit context: ExecutionContext) = {
    scrollByGazetteer(gazetteer, { place =>
      Logger.info(place.labels.toString)

      // TODO for each place, check if there's a geotag referencing it
      // if so, keep it (and log a warning)
      // if not, delete the record. That means:
      // - delete the whole place, if the record is the only one in this place
      // - update the record if there are any other records in this place

      Future.successful(true)
    })
  }

}

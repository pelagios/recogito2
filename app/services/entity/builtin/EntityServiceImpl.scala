package services.entity.builtin

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.RichSearchResponse
import com.vividsolutions.jts.geom.{Coordinate, Envelope}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.elasticsearch.common.geo.GeoPoint
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms
import org.elasticsearch.search.aggregations.metrics.geobounds.GeoBounds
import org.elasticsearch.search.sort.SortOrder
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import services.{HasTryToEither, Page}
import services.entity.{Entity, EntityType}
import storage.es.{ES, HasAggregations}

@Singleton
class EntityServiceImpl @Inject()(
  implicit val ctx: ExecutionContext,
  implicit val es: ES
) extends EntityService with EntityDeleteImpl with HasAggregations {

  implicit object EntityIndexable extends Indexable[Entity] {
    override def json(e: Entity): String =
      try { Json.stringify(Json.toJson(e)) } catch { case t: Throwable =>
        play.api.Logger.info(e.toString)
        throw t
      }
  }

  implicit object EntityHitReader extends HitReader[IndexedEntity] with HasTryToEither {
    override def read(hit: Hit): Either[Throwable, IndexedEntity] = {
      val e = Json.fromJson[Entity](Json.parse(hit.sourceAsString))
      Try(IndexedEntity(e.get, Some(hit.version)))
    }
  }

  private def toPage(response: RichSearchResponse, offset: Int, limit: Int): Page[IndexedEntity] =
    Page(response.tookInMillis, response.totalHits, offset, limit, response.to[IndexedEntity])

  override def countEntities(eType: Option[EntityType] = None): Future[Long] = eType match {
    case Some(t) =>
      es.client execute {
        search(ES.RECOGITO / ES.ENTITY) query {
          termQuery("entity_type" -> t.toString)
        } limit 0
      } map { _.totalHits }

    case _ =>
      es.client execute {
        search(ES.RECOGITO / ES.ENTITY) limit 0
      } map { _.totalHits }
  }

  override def countByAuthority(eType: Option[EntityType] = None): Future[Seq[(String, Long)]] = {
    val base = eType match {
      case Some(t) =>
          search(ES.RECOGITO / ES.ENTITY) query termQuery("entity_type" -> t.toString)
      case _ =>
          search(ES.RECOGITO / ES.ENTITY)
    }

    es.client execute {
      base size 0 aggs (
        termsAggregation("by_authority") field ("is_conflation_of.source_authority") size ES.MAX_SIZE
      )
    } map { response =>
      parseTermsAggregation(response.aggregations.termsResult("by_authority")).toSeq
    }
  }

  override def upsertEntities(entities: Seq[IndexedEntity]): Future[Boolean] = {
    val queries = entities.map { e =>
      e.version match {
        case Some(version) =>
          update(e.entity.unionId.toString) in ES.RECOGITO / ES.ENTITY doc e.entity version version

        case None =>
          indexInto(ES.RECOGITO / ES.ENTITY) id e.entity.unionId.toString doc e.entity
      }
    }

    if (queries.isEmpty) {
      Future.successful(true)
    } else {
      es.client.java.prepareBulk()
      es.client execute {
        bulk (queries)
      } map { ES.logFailures(_)
      } recover { case t: Throwable =>
        Logger.info(entities.toString)
        t.printStackTrace
        false
      }
    }
  }

  override def deleteEntities(ids: Seq[UUID]): Future[Boolean] = {
    if (ids.isEmpty) {
      Future.successful(true)
    } else {
      es.client.java.prepareBulk()
      es.client execute {
        bulk(ids.map { id =>
          delete(id.toString) from ES.RECOGITO / ES.ENTITY
        })
      } map { ES.logFailures(_) }
    }
  }

  override def findByURI(uri: String): Future[Option[IndexedEntity]] =
    es.client execute {
      search(ES.RECOGITO / ES.ENTITY) query termQuery("is_conflation_of.uri" -> uri) limit 2
    } map { _.to[IndexedEntity].toList match {
      case Nil => None
      case Seq(e) => Some(e)
      case results  =>
        Logger.warn(s"Search for ${uri} returned ${results.size} results")
        None
    }}

  override def findConnected(uris: Seq[String]): Future[Seq[IndexedEntity]] =
    es.client execute {
      search(ES.RECOGITO / ES.ENTITY) query boolQuery.should {
        uris.map(uri => termQuery("is_conflation_of.uri" -> uri)) ++
        uris.map(uri => termQuery("is_conflation_of.links.uri" -> uri))
      } version true limit ES.MAX_SIZE
    } map { _.to[IndexedEntity] }

  override def searchEntities(
    q: String,
    eType: Option[EntityType] = None,
    offset: Int = 0,
    limit: Int = ES.MAX_SIZE,
    sortFrom: Option[Coordinate] = None,
    authorities : Option[Seq[String]] = None
  ): Future[Page[IndexedEntity]] = {
    
    val baseQuery = boolQuery.should(
      // Treat as standard query string query first...
      queryStringQuery(q).defaultOperator("AND"),

      // ...and then look for exact matches in specific fields
      boolQuery.should(
        // Search inside record titles...
        matchPhraseQuery("is_conflation_of.title.raw", q).boost(5.0),
        matchPhraseQuery("is_conflation_of.title", q),

        // ...names...
        matchPhraseQuery("is_conflation_of.names.name.raw", q).boost(5.0),
        matchPhraseQuery("is_conflation_of.names.name", q),

        // ...and descriptions (with lower boost)
        matchQuery("is_conflation_of.descriptions.description", q).operator("AND").boost(0.2)
      )
    )

    val query =
      search(ES.RECOGITO / ES.ENTITY) query {
        authorities match {
          case Some(allowed) =>
            boolQuery.must(
              boolQuery.should(allowed.map(id => matchQuery("is_conflation_of.source_authority", id))),
              baseQuery
            )
          case None =>
            baseQuery
        }
    } start offset limit limit

    es.client execute {
     sortFrom match {
       case None => query
       case Some(coord) =>
         query sortBy (geoSort("representative_point") points(new GeoPoint(coord.y, coord.x)) order SortOrder.ASC)
     }
    } map { toPage(_, offset, limit) }
  }
  
  /** Helper that aggregates the entity IDs referenced by the annotations
    * in a given document.
    */
  private def aggregateEntityIds(docId: String): Future[Map[String, Long]] =
    es.client execute {
      search (ES.RECOGITO / ES.ANNOTATION) query {
        termQuery("annotates.document_id" -> docId)
      } aggregations (
        nestedAggregation("per_body", "bodies") subaggs (
          termsAggregation("by_union_id") field("bodies.reference.union_id") size ES.MAX_SIZE
        )
      ) size 0
    } map { response =>
      val terms = response.aggregations
        .getAs[InternalNested]("per_body")
        .getAggregations.get[StringTerms]("by_union_id")
      parseTermsAggregation(terms)
    }

  /** Lists entities in a document.
    *
    * Since we don't use ES parent/child relations via an associative entity any more,
    * this now happens via a two-stage process:
    *
    * 1. entity identifiers are aggregated from the annotations on the document
    * 2. union IDs are resolved against the index using a multiget query
    */
  override def listEntitiesInDocument(docId: String, eType: Option[EntityType] = None,
    offset: Int = 0, limit: Int = ES.MAX_SIZE): Future[Page[(IndexedEntity, Long)]] = {

    val startTime = System.currentTimeMillis
    
    val fAggregateIds = aggregateEntityIds(docId)

    def resolveEntities(unionIds: Seq[String]): Future[Seq[IndexedEntity]] =
      if (unionIds.isEmpty)
        Future.successful(Seq.empty[IndexedEntity])
      else
        es.client execute {
          multiget (
            unionIds.map { id => get(id) from ES.RECOGITO / ES.ENTITY }
          )
        } map { _.items.map(_.to[IndexedEntity]) }

    val f = for {
      counts <- fAggregateIds
      entities <- resolveEntities(counts.map(_._1).toSeq)
    } yield (counts, entities)

    f.map { case (counts, entities) =>
      val took = System.currentTimeMillis - startTime

      val zipped = counts.zip(entities).map { case ((unionId, count), entity) =>
        (entity, count)
      }.toSeq

      Page(took, 0l, 0, ES.MAX_SIZE, zipped)
    }
  }
  
  override def getDocumentSpatialExtent(docId: String): Future[Envelope] = {
    val fAggregateIds = aggregateEntityIds(docId)
    
    def toCoord(pt: GeoPoint) = new Coordinate(pt.getLon, pt.getLat)
    
    def aggregateCoverage(unionIds: Seq[String]) =
      if (unionIds.isEmpty)
        Future.successful(new Envelope())
      else
        es.client execute {
          search(ES.RECOGITO / ES.ENTITY) query {
            boolQuery.should (
              unionIds.map(id => matchQuery("union_id", id))
            )
          } aggregations (
            geoBoundsAggregation("point_bounds") field ("representative_point")
          ) size 0
        } map { response =>
          val bounds = response.aggregations.getAs[GeoBounds]("point_bounds")
          new Envelope(toCoord(bounds.bottomRight), toCoord(bounds.topLeft))
        }
  
    for {
      counts <- fAggregateIds
      envelope <- aggregateCoverage(counts.map(_._1).toSeq.take(ES.MAX_CLAUSES))
    } yield (envelope)
  }

  override def searchEntitiesInDocument(query: String, docId: String, eType: Option[EntityType] = None,
    offset: Int = 0, limit: Int = ES.MAX_SIZE): Future[Page[IndexedEntity]] = ???

}

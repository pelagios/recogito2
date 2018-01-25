package services.entity.builtin.importer

import collection.JavaConverters._
import com.vividsolutions.jts.geom.Coordinate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.{ExecutionContext, Future}
import services.Page
import services.entity.{Entity, EntityRecord, EntityType}
import services.entity.builtin.{EntityService, IndexedEntity}
import storage.es.ES

class MockEntityService(implicit ctx: ExecutionContext) extends EntityService {

  val mockIndex = new ConcurrentHashMap[UUID, Entity]

  override def countEntities(eType: Option[EntityType] = None): Future[Long] =
    eType match {
      case Some(entityType) => ???
      case None => Future.successful(mockIndex.size)
    }

  override def upsertEntities(entities: Seq[IndexedEntity]): Future[Boolean] =
    Future {
      entities.foreach { e =>
        mockIndex.put(e.entity.unionId, e.entity)
      }
      true
    }

  override def deleteEntities(ids: Seq[UUID]): Future[Boolean] =
    Future {
      ids.foldLeft(true){ case (success, id) =>
        val isDefined = Option(mockIndex.remove(id)).isDefined
        success && isDefined
      }
    }

  override def findByURI(uri: String): Future[Option[IndexedEntity]] =
    Future {
      val normalizedURI = EntityRecord.normalizeURI(uri)

      Option(mockIndex.get(normalizedURI)) match {
        case Some(hit) => Some(IndexedEntity(hit))

        case None => // Might still be listed under alternative record URI
          mockIndex.asScala.values.find { entity =>
            entity.uris.contains(normalizedURI)
          } map { IndexedEntity(_) }
      }
    }

  override def findConnected(uris: Seq[String]): Future[Seq[IndexedEntity]] =
    Future {
      val normalized = uris.map(uri => EntityRecord.normalizeURI(uri)).toSet
      mockIndex.asScala.values.filter { entity =>
        val allIdsAndMatches = entity.uris ++ entity.links.map(_.uri)
        allIdsAndMatches.exists(normalized.contains(_))
      }.toSeq.map { IndexedEntity(_) }
    }

  override def searchEntities(
    query    : String,
    eType    : Option[EntityType] = None,
    offset   : Int = 0,
    limit    : Int = ES.MAX_SIZE,
    sortFrom : Option[Coordinate] = None): Future[Page[IndexedEntity]] =

    Future {
      val results = mockIndex.asScala.values.toSeq
        .filter { entity =>
          val names = entity.names.keys.toSeq.map(_.name.toLowerCase)
          names.contains(query.toLowerCase)
        } map { IndexedEntity(_) }

      Page(0l, results.size, 0, limit, results.take(limit))
    }

  /** Unimplemented methods **/
  override def listAuthorities(eType: Option[EntityType] = None): Future[Seq[(String, Long)]] = ???

  override def listEntitiesInDocument(docId  : String,
    eType  : Option[EntityType] = None,
    offset : Int = 0,
    limit  : Int = ES.MAX_SIZE
  ): Future[Page[(IndexedEntity, Long)]] = ???

  def searchEntitiesInDocument(
    query  : String,
    docId  : String,
    eType  : Option[EntityType] = None,
    offset : Int = 0,
    limit  : Int = ES.MAX_SIZE
  ): Future[Page[IndexedEntity]] = ???

  def deleteBySourceAuthority(authority: String): Future[Boolean] = ???

}

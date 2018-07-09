package services.entity.builtin

import com.vividsolutions.jts.geom.Coordinate
import java.util.UUID
import scala.concurrent.Future
import services.Page
import services.entity.{Entity, EntityType}
import storage.es.ES

trait EntityService {

  def countEntities(eType: Option[EntityType] = None): Future[Long]

  def countByAuthority(eType: Option[EntityType] = None): Future[Seq[(String, Long)]]

  def upsertEntities(entities: Seq[IndexedEntity]): Future[Boolean]

  def deleteEntities(ids: Seq[UUID]): Future[Boolean]

  def findByURI(uri: String): Future[Option[IndexedEntity]]

  def findConnected(uris: Seq[String]): Future[Seq[IndexedEntity]]

  def searchEntities(
    query       : String,
    eType       : Option[EntityType] = None,
    offset      : Int = 0,
    limit       : Int = ES.MAX_SIZE,
    sortFrom    : Option[Coordinate] = None,
    authorities : Option[Seq[String]] = None
  ): Future[Page[IndexedEntity]]

  def listEntitiesInDocument(
    docId  : String,
    eType  : Option[EntityType] = None,
    offset : Int = 0,
    limit  : Int = ES.MAX_SIZE
  ): Future[Page[(IndexedEntity, Long)]]

  def searchEntitiesInDocument(
    query  : String,
    docId  : String,
    eType  : Option[EntityType] = None,
    offset : Int = 0,
    limit  : Int = ES.MAX_SIZE
  ): Future[Page[IndexedEntity]]

  def deleteBySourceAuthority(authority: String): Future[Boolean]

}

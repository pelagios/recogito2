package services.entity

import com.vividsolutions.jts.geom.Coordinate
import java.util.UUID
import scala.concurrent.Future
import services.Page
import storage.es.ES

case class IndexedEntity(entity: Entity, version: Option[Long] = None)

trait EntityService {
  
  def countEntities(eType: Option[EntityType] = None): Future[Long]
  
  def listAuthorities(eType: Option[EntityType] = None): Future[Seq[(String, Long)]]
  
  def upsertEntities(entities: Seq[IndexedEntity]): Future[Boolean]
  
  def deleteEntities(ids: Seq[UUID]): Future[Boolean]
  
  def findByURI(uri: String): Future[Option[IndexedEntity]]
  
  def findConnected(uris: Seq[String]): Future[Seq[IndexedEntity]]
  
  def searchEntities(
    query    : String, 
    eType    : Option[EntityType] = None,
    offset   : Int = 0, 
    limit    : Int = ES.MAX_SIZE, 
    sortFrom : Option[Coordinate] = None
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
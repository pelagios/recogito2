package services.entity.builtin

import services.entity.Entity

/** Utility container for an entity + its version in ElasticSearch
  *
  * Note: version is needed exclusively for supporting optimistic locking
  * during updates.  
  */
case class IndexedEntity(entity: Entity, version: Option[Long] = None)
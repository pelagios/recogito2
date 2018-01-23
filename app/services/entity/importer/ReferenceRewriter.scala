package services.entity.importer

import scala.concurrent.Future
import services.entity.IndexedEntity

/** Interface for the ReferenceRewriter
  *
  * Not much to see here. Just provides a single method that triggers rewriting of annotations
  * in the index, based on a provided set of entity updates.
  * 
  * We split this out into interface/implementation so we can mock this out in the unit test
  * that validates the importer.
  */
trait ReferenceRewriter {

  def rewriteReferencesTo(entitiesBefore: Seq[IndexedEntity], entitiesAfter: Seq[IndexedEntity]): Future[Boolean]
    
}
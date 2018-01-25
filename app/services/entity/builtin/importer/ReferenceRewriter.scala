package services.entity.builtin.importer

import scala.concurrent.Future
import services.entity.builtin.IndexedEntity

/** Interface for the ReferenceRewriter.
  *
  * Not much to see here. Just provides a single method that triggers rewriting of annotations
  * in the index, based on a provided set of entity updates. The reason we've split this out into
  * interface/implementation is just so that we can mock it in the unit test for the importer.
  */
trait ReferenceRewriter {

  def rewriteReferencesTo(entitiesBefore: Seq[IndexedEntity], entitiesAfter: Seq[IndexedEntity]): Future[Boolean]
    
}
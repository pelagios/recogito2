package services.entity.importer

import services.entity.IndexedEntity
import scala.concurrent.Future

class MockReferenceRewriter extends ReferenceRewriter {
  
  override def rewriteReferencesTo(entitiesBefore: Seq[IndexedEntity], entitiesAfter: Seq[IndexedEntity]) = 
    Future.successful(true)
  
}
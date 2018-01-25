package services.entity.builtin.importer

import scala.concurrent.Future
import services.entity.builtin.IndexedEntity

class MockReferenceRewriter extends ReferenceRewriter {

  override def rewriteReferencesTo(entitiesBefore: Seq[IndexedEntity], entitiesAfter: Seq[IndexedEntity]) =
    Future.successful(true)

}

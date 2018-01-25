package services.entity.builtin.importer

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import services.entity.EntityType
import services.entity.builtin.EntityService
import storage.es.ES

/** Helper class to generate new importer instances.
  *
  * Most of the dependencies we need for an importer can be injected:
  * execution context, ElasticSearch, the default entity service implementation.
  * One thing we want to be able to define in code, however, is the entity type.
  * Also, we want to be able to replace the rewriter with a mock for testing.
  *
  * This factory allows us to generate a new importer instance for a given entity
  * type with all the defaults injected. (Whereas in a unit test, we can just build
  * our own importer instance, bypassing injection.)
  */
@Singleton
class EntityImporterFactory @Inject()(
  ctx           : ExecutionContext,
  entityService : EntityService,
  es            : ES,
  rewriter      : ReferenceRewriter
) {

  def createImporter(eType: EntityType) =
    new EntityImporter(entityService, rewriter, eType, es, ctx)

}

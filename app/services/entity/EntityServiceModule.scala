package services.entity

import com.google.inject.AbstractModule
import services.entity.importer.{ReferenceRewriter, ReferenceRewriterImpl}

class EntityServiceModule extends AbstractModule {
  
  def configure() = {
    bind(classOf[EntityService]).to(classOf[EntityServiceImpl])
    bind(classOf[ReferenceRewriter]).to(classOf[ReferenceRewriterImpl])
  }
  
}
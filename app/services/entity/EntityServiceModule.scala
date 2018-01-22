package services.entity

import com.google.inject.AbstractModule

class EntityServiceModule extends AbstractModule {
  
  def configure() =
    bind(classOf[EntityService]).to(classOf[EntityServiceImpl])
  
}
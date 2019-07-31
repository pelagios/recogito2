package controllers.document.downloads.serializers.places

import controllers.document.downloads.serializers.BaseSerializer
import scala.concurrent.ExecutionContext
import services.annotation.{AnnotationService, AnnotationBody}
import services.entity.EntityType
import services.entity.builtin.EntityService
import storage.es.ES

trait PlacesToKML extends BaseSerializer {

  def placesToKML(
    documentId: String
  )(implicit 
      entityService: EntityService, 
      annotationService: AnnotationService, 
      ctx: ExecutionContext
  ) = {
  
    // TODO

  }

}
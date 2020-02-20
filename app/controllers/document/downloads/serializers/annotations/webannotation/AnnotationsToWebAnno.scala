package controllers.document.downloads.serializers.annotations.webannotation

import controllers.document.downloads.serializers.BaseSerializer
import services.ContentType
import services.annotation.{AnnotationService, AnnotationBody}
import services.document.{ExtendedDocumentMetadata, DocumentService}
import services.entity.EntityType
import services.entity.builtin.EntityService
import play.api.mvc.{AnyContent, Request}
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import storage.es.ES 

trait AnnotationsToWebAnno extends BaseSerializer {

  def documentToWebAnnotation(
    doc: ExtendedDocumentMetadata
  )(implicit 
    documentService: DocumentService,
    entityService: EntityService,
    annotationService: AnnotationService,
    request: Request[AnyContent], 
    ctx: ExecutionContext
  ) = {
    // To be used as 'generator' URI
    val recogitoURI = controllers.landing.routes.LandingController.index().absoluteURL

    val fAnnotations = annotationService.findByDocId(doc.id, 0, ES.MAX_SIZE)
    val fPlaces = entityService.listEntitiesInDocument(doc.id, Some(EntityType.PLACE), 0, ES.MAX_SIZE)

    val f = for {
      annotations <- fAnnotations
      places <- fPlaces
    } yield (annotations.map(_._1), places.items.map(_._1.entity))

    f.map { case (annotations, places) =>
      Json.toJson(annotations.map { annotation =>
        val filepart = doc.fileparts.find(_.getId == annotation.annotates.filepartId).get

        val placesOnThisAnnotation = annotation.bodies
          .filter(_.hasType == AnnotationBody.PLACE)
          .filter(_.uri.isDefined)
          .flatMap(body => {
            places.find(entity => entity.uris.contains(body.uri.get))
          })

        WebAnnotation(filepart, recogitoURI, annotation, placesOnThisAnnotation)
      })
    }
  }

}

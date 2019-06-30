package controllers.document.downloads.serializers.annotations.webannotation

import controllers.document.downloads.serializers.BaseSerializer
import services.ContentType
import services.annotation.AnnotationService
import services.document.{ ExtendedDocumentMetadata, DocumentService }
import play.api.mvc.{ AnyContent, Request }
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext

trait AnnotationsToWebAnno extends BaseSerializer {

  def documentToWebAnnotation(doc: ExtendedDocumentMetadata)(implicit documentService: DocumentService,
      annotationService: AnnotationService, request: Request[AnyContent], ctx: ExecutionContext) = {

    // To be used as 'generator' URI
    val recogitoURI = controllers.landing.routes.LandingController.index().absoluteURL

    annotationService.findByDocId(doc.id).map { annotations =>
      Json.toJson(annotations.map { case (annotation, _) =>
        val filepart = doc.fileparts.find(_.getId == annotation.annotates.filepartId).get
        WebAnnotation(filepart, recogitoURI, annotation)
      })
    }
  }

}

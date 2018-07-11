package controllers.document.downloads.serializers.annotations.webannotation

import controllers.document.downloads.serializers.BaseSerializer
import services.annotation.AnnotationService
import services.document.{ DocumentInfo, DocumentService }
import play.api.mvc.{ AnyContent, Request }
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext

trait AnotationsToWebAnno extends BaseSerializer {

  def documentToWebAnnotation(doc: DocumentInfo)(implicit documentService: DocumentService,
      annotationService: AnnotationService, request: Request[AnyContent], ctx: ExecutionContext) = {

    val recogitoURI = controllers.landing.routes.LandingController.index().absoluteURL
    val documentURI = controllers.document.routes.DocumentController.initialDocumentView(doc.id).absoluteURL

    annotationService.findByDocId(doc.id).map { annotations =>
      Json.toJson(annotations.map { case (annotation, _) =>
        WebAnnotation(recogitoURI, documentURI, annotation)
      })
    }
  }

}

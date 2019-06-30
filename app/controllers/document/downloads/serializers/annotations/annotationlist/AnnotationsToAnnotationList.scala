package controllers.document.downloads.serializers.annotations.annotationlist

import controllers.document.downloads.serializers.BaseSerializer
import services.ContentType
import services.annotation.{Annotation, AnnotationService}
import services.document.{ExtendedDocumentMetadata, DocumentService}
import play.api.mvc.{AnyContent, Request}
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext

trait AnnotationsToAnnotationList extends BaseSerializer {

  // TODO implement
  private def toAnnotationResource(annotation: Annotation) = AnnotationResource("foo", "bar")

  def documentToIIIF2(doc: ExtendedDocumentMetadata)(implicit documentService: DocumentService,
      annotationService: AnnotationService, request: Request[AnyContent], ctx: ExecutionContext) = {

    // To be used as 'generator' URI
    val recogitoURI = controllers.landing.routes.LandingController.index().absoluteURL

    annotationService.findByDocId(doc.id).map { annotations =>
      Json.toJson(AnnotationList(recogitoURI, annotations.map(t => toAnnotationResource(t._1))))
    }
  }

}

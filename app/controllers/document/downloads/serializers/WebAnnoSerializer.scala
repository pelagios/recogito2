package controllers.document.downloads.serializers

import models.annotation.{ Annotation, AnnotationService }
import models.document.{ DocumentInfo, DocumentService }
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{ AnyContent, Request }
import scala.concurrent.ExecutionContext

trait WebAnnoSerializer extends BaseSerializer {
  
  def documentToWebAnnotation(doc: DocumentInfo)(implicit documentService: DocumentService,
      annotationService: AnnotationService, request: Request[AnyContent], ctx: ExecutionContext) = {
    
    val baseUri = controllers.document.routes.DocumentController.initialDocumentView(doc.id).absoluteURL 
    
    annotationService.findByDocId(doc.id).map(annotations =>
      Json.toJson(annotations.map(t => WebAnnotation(baseUri, t._1))))
  }
  
}


case class WebAnnotation(baseURI: String, annotation: Annotation)

object WebAnnotation {
  
  implicit val webAnnotationWrites: Writes[WebAnnotation] = (
    (JsPath \ "@context").write[String] and
    (JsPath \ "id").write[String] and
    (JsPath \ "type").write[String]
  )(a => (
    "http://www.w3.org/ns/anno.jsonld",
    a.baseURI + "#" + a.annotation.annotationId.toString,
   "Annotation"
  ))

}
package controllers.document.downloads.serializers

import models.HasDate
import models.annotation.{ Annotation, AnnotationBody, AnnotationService }
import models.document.{ DocumentInfo, DocumentService }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{ AnyContent, Request }
import scala.concurrent.ExecutionContext

trait WebAnnotationSerializer extends BaseSerializer {
  
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

case class WebAnnotationBody(
  hasType : Option[WebAnnotationBody.Value],
  id      : Option[String], 
  value   : Option[String],
  creator : Option[String],
  modified: DateTime
)
  
object WebAnnotationBody extends Enumeration with HasDate {
  
  val TextualBody = Value("TextualBody")
  
  /** Note we don't explicitely serialize QUOTE bodies **/
  def fromAnnotationBody(b: AnnotationBody, recogitoBaseURI: String): Option[WebAnnotationBody] = {
    
    import AnnotationBody._
    
    val hasType = b.hasType match {
      case COMMENT | TAG | QUOTE | TRANSCRIPTION => Some(TextualBody)
      case _ => None
    }
    
    if (b.hasType == QUOTE)
      None
    else
      Some(WebAnnotationBody(
        hasType,
        b.uri,
        b.value,
        b.lastModifiedBy.map(by => recogitoBaseURI + by),
        b.lastModifiedAt))
  }
  
  implicit val webAnnotationBodyWrites: Writes[WebAnnotationBody] = (
    (JsPath \ "type").writeNullable[WebAnnotationBody.Value] and
    (JsPath \ "id").writeNullable[String] and
    (JsPath \ "value").writeNullable[String] and
    (JsPath \ "creator").writeNullable[String] and
    (JsPath \ "modified").write[DateTime]
  )(unlift(WebAnnotationBody.unapply))
  
}

case class WebAnnotation(recogitoBaseURI: String, documentURI: String, annotation: Annotation)

object WebAnnotation extends HasDate {
  
  implicit val webAnnotationWrites: Writes[WebAnnotation] = (
    (JsPath \ "@context").write[String] and
    (JsPath \ "id").write[String] and
    (JsPath \ "type").write[String] and
    (JsPath \ "generator").write[String] and
    (JsPath \ "generated").write[DateTime] and
    (JsPath \ "body").write[Seq[WebAnnotationBody]]
  )(a => (
    "http://www.w3.org/ns/anno.jsonld",
    a.documentURI + "#" + a.annotation.annotationId.toString,
    "Annotation",
    a.recogitoBaseURI,
    DateTime.now,
    a.annotation.bodies.flatMap(b => WebAnnotationBody.fromAnnotationBody(b, a.recogitoBaseURI))
  ))

}
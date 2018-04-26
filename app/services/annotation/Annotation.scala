package services.annotation

import java.util.UUID
import services.{ ContentType, HasContentTypeList, HasDate }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.generated.tables.records.DocumentFilepartRecord

case class Annotation(
  annotationId: UUID,
  versionId: UUID,
  annotates: AnnotatedObject,
  contributors: Seq[String],
  anchor: String,
  lastModifiedBy: Option[String],
  lastModifiedAt: DateTime,
  bodies: Seq[AnnotationBody]) {
  
  def withBody(body: AnnotationBody) = 
    this.copy(bodies = this.bodies :+ body.copy(lastModifiedAt = this.lastModifiedAt))
  
}

case class AnnotatedObject(documentId: String, filepartId: UUID, contentType: ContentType)

object AnnotatedObject extends HasContentTypeList {

  implicit val annotatedObjectFormat: Format[AnnotatedObject] = (
    (JsPath \ "document_id").format[String] and
    (JsPath \ "filepart_id").format[UUID] and
    (JsPath \ "content_type").format[JsValue].inmap[ContentType](fromCTypeList, toCTypeList)
  )(AnnotatedObject.apply, unlift(AnnotatedObject.unapply))

}

object BackendAnnotation extends HasDate {
  
  // Backend body serialization
  import services.annotation.BackendAnnotationBody._
  
  implicit val backendAnnotationFormat: Format[Annotation] = (
    (JsPath \ "annotation_id").format[UUID] and
    (JsPath \ "version_id").format[UUID] and
    (JsPath \ "annotates").format[AnnotatedObject] and
    (JsPath \ "contributors").format[Seq[String]] and
    (JsPath \ "anchor").format[String] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime] and
    (JsPath \ "bodies").format[Seq[AnnotationBody]]
  )(Annotation.apply, unlift(Annotation.unapply))

}

object FrontendAnnotation extends HasDate {

  // Frontend body serialization
  import services.annotation.FrontendAnnotationBody._
  
  implicit val frontendAnnotationWrites: Writes[Annotation] = (
    (JsPath \ "annotation_id").write[UUID] and
    (JsPath \ "version_id").write[UUID] and
    (JsPath \ "annotates").write[AnnotatedObject] and
    (JsPath \ "contributors").write[Seq[String]] and
    (JsPath \ "anchor").write[String] and
    (JsPath \ "last_modified_by").writeNullable[String] and
    (JsPath \ "last_modified_at").write[DateTime] and
    (JsPath \ "bodies").write[Seq[AnnotationBody]]
  )(unlift(Annotation.unapply))
  
}

/** Helpers for creating some standard annotation formats **/
object Annotation {
  
  def on(part: DocumentFilepartRecord, anchor: String) =
    Annotation(
      UUID.randomUUID(),
      UUID.randomUUID(),
      AnnotatedObject(part.getDocumentId, part.getId, ContentType.withName(part.getContentType).get),
      Seq.empty[String], // contributors
      anchor,
      None, // lastModifiedBy
      new DateTime(),
      Seq.empty[AnnotationBody])

}

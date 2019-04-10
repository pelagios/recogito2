package services.annotation

import java.util.UUID
import services.{ ContentType, HasContentTypeList, HasDate }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasNullableSeq
import services.annotation.relation.Relation
import services.generated.tables.records.DocumentFilepartRecord

case class Annotation(
  annotationId: UUID,
  versionId: UUID,
  annotates: AnnotatedObject,
  contributors: Seq[String],
  anchor: String,
  lastModifiedBy: Option[String],
  lastModifiedAt: DateTime,
  bodies: Seq[AnnotationBody],
  relations: Seq[Relation]) {
  
  def withBody(body: AnnotationBody) = 
    this.copy(bodies = this.bodies :+ body.copy(lastModifiedAt = this.lastModifiedAt))

  def cloneTo(docId: String, filepartId: UUID) = 
    this.copy(
      annotationId = UUID.randomUUID,
      versionId = UUID.randomUUID,
      annotates = 
        AnnotatedObject(docId, filepartId, this.annotates.contentType))

}

case class AnnotatedObject(documentId: String, filepartId: UUID, contentType: ContentType)

object AnnotatedObject extends HasContentTypeList {

  implicit val annotatedObjectFormat: Format[AnnotatedObject] = (
    (JsPath \ "document_id").format[String] and
    (JsPath \ "filepart_id").format[UUID] and
    (JsPath \ "content_type").format[JsValue].inmap[ContentType](fromCTypeList, toCTypeList)
  )(AnnotatedObject.apply, unlift(AnnotatedObject.unapply))

}

object BackendAnnotation extends HasDate with HasNullableSeq {
  
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
    (JsPath \ "bodies").format[Seq[AnnotationBody]] and
    (JsPath \ "relations").formatNullable[Seq[Relation]]
      .inmap[Seq[Relation]](fromOptSeq, toOptSeq)
  )(Annotation.apply, unlift(Annotation.unapply))

}

object FrontendAnnotation extends HasDate with HasNullableSeq {

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
    (JsPath \ "bodies").write[Seq[AnnotationBody]] and
    (JsPath \ "relations").writeNullable[Seq[Relation]]
      .contramap[Seq[Relation]](toOptSeq)
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
      Seq.empty[AnnotationBody],
      Seq.empty[Relation])

}

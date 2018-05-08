package controllers.api.stubs

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasNullableSeq
import services.annotation.AnnotatedObject

case class AnnotationStub(
  annotationId: Option[UUID],
  annotates   : AnnotatedObject,
  anchor      : String,
  bodies      : Seq[AnnotationBodyStub],
  relations   : Seq[RelationStub])

object AnnotationStub extends HasNullableSeq {

  implicit val annotationStubReads: Reads[AnnotationStub] = (
    (JsPath \ "annotation_id").readNullable[UUID] and
    (JsPath \ "annotates").read[AnnotatedObject] and
    (JsPath \ "anchor").read[String] and
    (JsPath \ "bodies").read[Seq[AnnotationBodyStub]] and
    (JsPath \ "relations").readNullable[Seq[RelationStub]]
      .map(fromOptSeq)
  )(AnnotationStub.apply _)

}
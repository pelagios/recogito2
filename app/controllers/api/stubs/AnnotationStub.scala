package controllers.api.stubs

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasNullableSeq
import services.annotation.{Annotation, AnnotatedObject}
import org.joda.time.DateTime

case class AnnotationStub(
  annotationId: Option[UUID],
  annotates   : AnnotatedObject,
  anchor      : String,
  bodies      : Seq[AnnotationBodyStub],
  relations   : Seq[RelationStub]) {
  
  def toAnnotation(user: String) = {
    val now = DateTime.now()
    Annotation(
      annotationId.getOrElse(UUID.randomUUID),
      UUID.randomUUID,
      annotates,
      (bodies.flatMap(_.lastModifiedBy) :+ user).distinct,
      anchor,
      Some(user),
      now,
      bodies.map(_.toAnnotationBody(user, now)),
      relations.map(_.toRelation(user, now)))
  }
  
}

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
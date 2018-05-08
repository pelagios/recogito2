package controllers.api.stubs

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasDate
import services.annotation.AnnotationBody

case class AnnotationBodyStub(
  hasType       : AnnotationBody.Type,
  lastModifiedBy: Option[String],
  lastModifiedAt: Option[DateTime],
  value         : Option[String],
  uri           : Option[String],
  note          : Option[String],
  status        : Option[AnnotationStatusStub]) {
  
  def toAnnotationBody(user: String, now: DateTime) =
    AnnotationBody(
      hasType,
      Some(user),
      now,
      value,
      uri,
      note,
      status.map(_.toAnnotationStatus(user, now)))
  
}

object AnnotationBodyStub extends HasDate {

  implicit val annotationBodyStubReads: Reads[AnnotationBodyStub] = (
    (JsPath \ "type").read[AnnotationBody.Value] and
    (JsPath \ "last_modified_by").readNullable[String] and
    (JsPath \ "last_modified_at").readNullable[DateTime] and
    (JsPath \ "value").readNullable[String] and
    (JsPath \ "uri").readNullable[String] and
    (JsPath \ "note").readNullable[String] and
    (JsPath \ "status").readNullable[AnnotationStatusStub]
  )(AnnotationBodyStub.apply _)

}
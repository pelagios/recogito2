package controllers.api.annotation.stubs

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasDate
import services.annotation.AnnotationStatus

case class AnnotationStatusStub(
  value: AnnotationStatus.Value,
  setBy: Option[String],
  setAt: Option[DateTime]) {
  
  def toAnnotationStatus(user: String, now: DateTime) =
    AnnotationStatus(
      value,
      Some(setBy.getOrElse(user)),
      setAt.getOrElse(now))

}

object AnnotationStatusStub extends HasDate {

  implicit val annotationStatusStubReads: Reads[AnnotationStatusStub] = (
    (JsPath \ "value").read[AnnotationStatus.Value] and
    (JsPath \ "set_by").readNullable[String] and
    (JsPath \ "set_at").readNullable[DateTime]
  )(AnnotationStatusStub.apply _)

}

package storage.es.migration

import services.HasDate
import services.annotation.{AnnotationBody, AnnotationStatus, Reference}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class LegacyAnnotationBody(
  hasType: AnnotationBody.Type,
  lastModifiedBy: Option[String],
  lastModifiedAt: DateTime,
  value: Option[String],
  uri: Option[String],
  note: Option[String],
  status: Option[AnnotationStatus]
) {

  def toNewAPI = AnnotationBody(
    hasType,
    lastModifiedBy,
    lastModifiedAt,
    value,
    uri.map(Reference(_)),
    note,
    status
  )

}

object LegacyAnnotationBody extends Enumeration with HasDate {

  implicit val legacyAnnotationBodyFormat: Format[LegacyAnnotationBody] = (
    (JsPath \ "type").format[AnnotationBody.Value] and
    (JsPath \ "last_modified_by").formatNullable[String] and
    (JsPath \ "last_modified_at").format[DateTime] and
    (JsPath \ "value").formatNullable[String] and
    (JsPath \ "uri").formatNullable[String] and
    (JsPath \ "note").formatNullable[String] and
    (JsPath \ "status").formatNullable[AnnotationStatus]
  )(LegacyAnnotationBody.apply, unlift(LegacyAnnotationBody.unapply))

}

package controllers.api.annotation.stubs

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasDate
import services.annotation.AnnotationBody
import services.annotation.relation.RelationBody

case class RelationBodyStub(
  hasType: AnnotationBody.Type,
  lastModifiedBy: Option[String],
  lastModifiedAt: Option[DateTime],
  value: String) {
  
  def toRelationBody(user: String, now: DateTime) =
    RelationBody(
      hasType, 
      Some(lastModifiedBy.getOrElse(user)), 
      lastModifiedAt.getOrElse(now),
      value)

}

object RelationBodyStub extends HasDate {
  
  implicit val relationBodyStubReads: Reads[RelationBodyStub] = (
    (JsPath \ "type").read[AnnotationBody.Type] and
    (JsPath \ "last_modified_by").readNullable[String] and
    (JsPath \ "last_modified_at").readNullable[DateTime] and
    (JsPath \ "value").read[String]
  )(RelationBodyStub.apply _)
  
}
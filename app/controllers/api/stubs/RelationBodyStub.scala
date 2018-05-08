package controllers.api.stubs

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.annotation.AnnotationBody
import services.annotation.relation.RelationBody

case class RelationBodyStub(hasType: AnnotationBody.Type, value: String) {
  
  def toRelationBody(user: String, now: DateTime) =
    RelationBody(hasType, Some(user), now, value)

}

object RelationBodyStub {
  
  implicit val relationBodyStubReads: Reads[RelationBodyStub] = (
    (JsPath \ "type").read[AnnotationBody.Type] and
    (JsPath \ "value").read[String]
  )(RelationBodyStub.apply _)
  
}
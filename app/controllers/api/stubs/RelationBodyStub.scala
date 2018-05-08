package controllers.api.stubs

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.annotation.AnnotationBody

case class RelationBodyStub(hasType: AnnotationBody.Type, value: String)

object RelationBodyStub {
  
  implicit val relationBodyStubReads: Reads[RelationBodyStub] = (
    (JsPath \ "type").read[AnnotationBody.Type] and
    (JsPath \ "value").read[String]
  )(RelationBodyStub.apply _)
  
}
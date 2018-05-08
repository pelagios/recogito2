package controllers.api.stubs

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasNullableSeq

case class RelationStub(relatesTo: UUID, relatesVia: Seq[UUID], bodies: Seq[RelationBodyStub])

object RelationStub extends HasNullableSeq {

  implicit val relationStubReads: Reads[RelationStub] = (
    (JsPath \ "relates_to").read[UUID] and
    (JsPath \ "relates_via").readNullable[Seq[UUID]]
      .map(fromOptSeq) and
    (JsPath \ "bodies").read[Seq[RelationBodyStub]]
  )(RelationStub.apply _)

}
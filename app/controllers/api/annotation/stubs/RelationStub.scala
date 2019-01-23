package controllers.api.annotation.stubs

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasNullableSeq
import services.annotation.relation.Relation

case class RelationStub(relatesTo: UUID, relatesVia: Seq[UUID], bodies: Seq[RelationBodyStub]) {
  
  def toRelation(user: String, now: DateTime) =
    Relation(relatesTo, relatesVia, bodies.map(_.toRelationBody(user, now)))

}

object RelationStub extends HasNullableSeq {

  implicit val relationStubReads: Reads[RelationStub] = (
    (JsPath \ "relates_to").read[UUID] and
    (JsPath \ "relates_via").readNullable[Seq[UUID]]
      .map(fromOptSeq) and
    (JsPath \ "bodies").read[Seq[RelationBodyStub]]
  )(RelationStub.apply _)

}
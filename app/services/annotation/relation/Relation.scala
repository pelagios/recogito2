package services.annotation.relation

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasNullableSeq

case class Relation(
  relatesTo: UUID,
  relatesVia: Seq[UUID],
  bodies: Seq[RelationBody]
) {

  def hasSamePath(other: Relation) = 
    relatesTo == other.relatesTo && relatesVia == other.relatesVia

}
  
object Relation extends HasNullableSeq {
  
  implicit val relationFormat: Format[Relation] = (
    (JsPath \ "relates_to").format[UUID] and
    (JsPath \ "relates_via").formatNullable[Seq[UUID]]
      .inmap[Seq[UUID]](fromOptSeq[UUID], toOptSeq[UUID]) and
    (JsPath \ "bodies").format[Seq[RelationBody]]
  )(Relation.apply, unlift(Relation.unapply))
  
}

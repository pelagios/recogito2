package transform.ner

import play.api.libs.json._
import play.api.libs.functional.syntax._
import transform.{JobDefinition, SpecificJobDefinition}

case class NERJobDefinition(
  protected val baseDef: JobDefinition,
  engine: String, 
  useAllAuthorities: Boolean,
  authorities: Seq[String]
) extends SpecificJobDefinition

object NERJobDefinition {

  implicit val georesolutionTaskDefinitionReads: Reads[NERJobDefinition] = (
    (JsPath).read[JobDefinition] and
    (JsPath \ "engine").read[String] and
    (JsPath \ "all_authorities").readNullable[Boolean]
      .map(_.getOrElse(false)) and 
    (JsPath \ "authorities").readNullable[Seq[String]]
      .map(_.getOrElse(Seq.empty[String]))
  )(NERJobDefinition.apply _)

}
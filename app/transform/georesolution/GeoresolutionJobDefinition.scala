package transform.georesolution

import play.api.libs.json._
import play.api.libs.functional.syntax._
import transform.{JobDefinition, SpecificJobDefinition}

case class GeoresolutionJobDefinition(
  val baseDef: JobDefinition, args: Map[String, String]
) extends SpecificJobDefinition

object GeoresolutionJobDefinition {

  implicit val georesolutionTaskDefinitionReads: Reads[GeoresolutionJobDefinition] = (
    (JsPath).read[JobDefinition] and
    (JsPath \ "args").read[Map[String, String]]
  )(GeoresolutionJobDefinition.apply _)

}
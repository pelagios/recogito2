package transform.mapkurator

import play.api.libs.json._
import play.api.libs.functional.syntax._
import transform.{JobDefinition, SpecificJobDefinition}

case class MapkuratorJobDefinition(
  baseDef: JobDefinition,
  version: String
) extends SpecificJobDefinition

object MapkuratorJobDefinition {

  implicit val mapkuratorJobDefinitionReads: Reads[MapkuratorJobDefinition] = (
    (JsPath).read[JobDefinition] and
    (JsPath \ "version").read[String]
  )(MapkuratorJobDefinition.apply _)

}
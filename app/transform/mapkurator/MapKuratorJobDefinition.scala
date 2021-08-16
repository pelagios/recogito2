package transform.mapkurator

import play.api.libs.json._
import play.api.libs.functional.syntax._
import transform.{JobDefinition, SpecificJobDefinition}

case class MapKuratorJobDefinition(
  baseDef: JobDefinition,
  minLon: Option[Double],
  minLat: Option[Double],
  maxLon: Option[Double],
  maxLat: Option[Double]
  ) extends SpecificJobDefinition

object MapKuratorJobDefinition {

  implicit val mapkuratorJobDefinitionReads: Reads[MapKuratorJobDefinition] = (
    (JsPath).read[JobDefinition] and
    (JsPath \ "minLon").readNullable[Double] and
    (JsPath \ "minLat").readNullable[Double] and
    (JsPath \ "maxLon").readNullable[Double] and
    (JsPath \ "maxLat").readNullable[Double]
  )(MapKuratorJobDefinition.apply _)

}
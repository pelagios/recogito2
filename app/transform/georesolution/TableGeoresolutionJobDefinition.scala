package transform.georesolution

import play.api.libs.json._
import play.api.libs.functional.syntax._
import transform.{JobDefinition, SpecificJobDefinition}

case class TableGeoresolutionJobDefinition(
  val baseDef: JobDefinition,
  val delimiter: Option[Char],
  toponymColumn: Int,
  latitudeColumn: Option[Int],
  longitudeColumn: Option[Int],
  useAllAuthorities: Boolean, 
  specificAuthorities: Seq[String]
) extends GeoresolutionJobDefinition

object TableGeoresolutionJobDefinition {

  implicit val tableGeoresolutionJobDefinitionReads: Reads[TableGeoresolutionJobDefinition] = (
    (JsPath).read[JobDefinition] and
    (JsPath \ "delimiter").readNullable[String].map(_.map(_.charAt(0))) and 
    (JsPath \ "toponym_column").read[Int] and
    (JsPath \ "lat_column").readNullable[Int] and
    (JsPath \ "lon_column").readNullable[Int] and 
    (JsPath \ "all_authorities").readNullable[Boolean].map(_.getOrElse(true)) and 
    (JsPath \ "authorities").readNullable[Seq[String]].map(_.getOrElse(Seq.empty[String]))
  )(TableGeoresolutionJobDefinition.apply _)

}
package transform.georesolution

import transform.{JobDefinition, SpecificJobDefinition}

case class GeoresolutionJobDefinition(
  val baseDef: JobDefinition,
  val useAllAuthorities: Boolean,
  val specificAuthorities: Seq[String]
) extends SpecificJobDefinition


package transform.georesolution

import transform.{JobDefinition, SpecificJobDefinition}

trait GeoresolutionJobDefinition extends SpecificJobDefinition {
  val baseDef: JobDefinition
  val useAllAuthorities: Boolean
  val specificAuthorities: Seq[String]
} 

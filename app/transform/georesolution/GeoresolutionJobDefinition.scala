package transform.georesolution

import java.util.UUID
import services.task.TaskType
import transform.{JobDefinition, SpecificJobDefinition}

trait GeoresolutionJobDefinition extends SpecificJobDefinition {
  val baseDef: JobDefinition
  val useAllAuthorities: Boolean
  val specificAuthorities: Seq[String]
} 

object GeoresolutionJobDefinition {

  // Creates an anonymous job definition with default values
  def default(documents : Seq[String], fileparts : Seq[UUID]) =
    new GeoresolutionJobDefinition {
      val baseDef = JobDefinition(TaskType("NER"), documents, fileparts)
      val useAllAuthorities = true
      val specificAuthorities = Seq.empty[String]
    }

}

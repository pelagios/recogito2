package transform

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.task.TaskType

case class JobDefinition (
  taskType: TaskType, 
  documents : Seq[String], 
  fileparts : Seq[UUID]
)

object JobDefinition {

  implicit val taskDefinitionReads: Reads[JobDefinition] = (
    (JsPath \ "task_type").read[String].map(str => TaskType(str)) and
    (JsPath \ "documents").read[Seq[String]] and
    (JsPath \ "fileparts").readNullable[Seq[UUID]]
      .map(_.getOrElse(Seq.empty[UUID]))
  )(JobDefinition.apply _)

}

trait SpecificJobDefinition {

  protected val baseDef: JobDefinition 

  val taskType = baseDef.taskType

  val documents = baseDef.documents

  val fileparts = baseDef.fileparts

}
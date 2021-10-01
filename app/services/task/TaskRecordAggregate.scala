package services.task

import java.sql.Timestamp
import java.util.UUID
import services.generated.tables.records.TaskRecord
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class TaskRecordAggregate(taskRecords: Seq[TaskRecord]) {
  
  private def getDistinctField[T](filter: TaskRecord => T, errorMessage: String): T = {
    val fields = taskRecords.map(filter).distinct
    if (fields.size != 1)
      throw new RuntimeException("Invalid task record aggregation: " + errorMessage + " (" + fields.mkString(", ") + ")")
    fields.head
  }

  lazy val taskType = TaskType(getDistinctField[String](_.getTaskType, "different task types"))

  lazy val className = getDistinctField[String](_.getClassName, "different class names")
  
  lazy val documentId = getDistinctField[String](_.getDocumentId, "different document IDs")
  
  lazy val spawnedBy = getDistinctField[String](_.getSpawnedBy, "different values for spawned_by")
  
  lazy val spawnedAt = taskRecords.sortBy(_.getSpawnedAt.getTime).head.getSpawnedAt
  
  lazy val stoppedAt = {
    val stoppedAtByTask = taskRecords.map(task => Option(task.getStoppedAt))
    if (stoppedAtByTask.exists(_.isEmpty))
      // At least one sub-task is unfinished - report aggregate task as unfinished 
      None
    else
      /// All stopped - use latest stop time
      Some(stoppedAtByTask.flatten.sortBy(_.getTime).reverse.head)
  }
  
  lazy val stoppedWith =
    taskRecords.flatMap(task => Option(task.getStoppedWith))

  lazy val status =
    taskRecords.map(task => TaskStatus.withName(task.getStatus)) match {
    
      case statusByTask if statusByTask.exists(_ == TaskStatus.FAILED) =>
        // Any task that failed?
        TaskStatus.FAILED
        
      case statusByTask if statusByTask.forall(_ == TaskStatus.COMPLETED) =>
        // All complete?
        TaskStatus.COMPLETED
        
      case statusByTask if statusByTask.forall(_ == TaskStatus.PENDING) =>
        // All pending?
        TaskStatus.PENDING
        
      case _ => TaskStatus.RUNNING
        
    }
  
  lazy val progress =
    taskRecords.map(_.getProgress.toInt).sum / taskRecords.size
  
}

object TaskRecordAggregate {
  
  implicit val taskRecordWrites: Writes[TaskRecord] = (
    (JsPath \ "task_type").write[String] and
    (JsPath \ "filepart_id").write[UUID] and
    (JsPath \ "status").write[String] and
    (JsPath \ "progress").write[Int] and
    (JsPath \ "message").writeNullable[String]
  )(r => (
     r.getTaskType,
     r.getFilepartId,
     r.getStatus,
     r.getProgress,
     Option(r.getStoppedWith)
  ))
  
  implicit val aggregateTaskRecordWrites: Writes[TaskRecordAggregate] = (
    (JsPath \ "document_id").write[String] and
    (JsPath \ "status").write[String] and
    (JsPath \ "progress").write[Int] and
    (JsPath \ "subtasks").write[Seq[TaskRecord]]
  )(r => (
      r.documentId,
      r.status.toString,
      r.progress,
      r.taskRecords
  ))  
  
}
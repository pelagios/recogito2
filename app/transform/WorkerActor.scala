package transform

import akka.actor.Actor
import java.io.File
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.task._

abstract class WorkerActor(
  taskType: TaskType, 
  taskService: TaskService
) extends Actor {
  
  def receive = {
    
    case msg: WorkerActor.WorkOnPart =>
      // Create a task record in the DB
      val taskId = Await.result(
        taskService.insertTask(
          taskType,
          this.getClass.getName,
          msg.jobId,
          Some(msg.document.getId),
          Some(msg.part.getId),
          Some(msg.document.getOwner)),
        10.seconds)
        
      taskService.updateTaskStatusAndProgress(taskId, TaskStatus.RUNNING, 1)
      
      // Actual work is left to the subclass to implement
      doWork(msg.document, msg.part, msg.dir, msg.jobDef, taskId)
      
      taskService.scheduleTaskForRemoval(taskId, 60.minutes)(context.system)
  }
  
  def doWork(
    doc: DocumentRecord, 
    part: DocumentFilepartRecord, 
    dir: File, 
    jobDef: Option[SpecificJobDefinition], 
    taskId: UUID)
  
}

object WorkerActor {
  
  case class WorkOnPart(
    jobId    : UUID,
    document : DocumentRecord,
    part     : DocumentFilepartRecord,
    dir      : File,
    jobDef   : Option[SpecificJobDefinition]) 
  
}
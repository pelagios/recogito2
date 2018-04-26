package transform

import akka.actor.Actor
import java.io.File
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.task._

abstract class WorkerActor(taskType: TaskType, taskService: TaskService) extends Actor {
  
  def receive = {
    
    case msg: WorkerActor.WorkOnPart =>
      
      val taskId = Await.result(
        taskService.insertTask(
          taskType,
          this.getClass.getName,
          Some(msg.document.getId),
          Some(msg.part.getId),
          Some(msg.document.getOwner)),
        10.seconds)
        
      taskService.updateStatusAndProgress(taskId, TaskStatus.RUNNING, 1)
      
      // Actual work is left to the subclass to implement
      doWork(msg.document, msg.part, msg.dir, msg.args, taskId)
      
      taskService.scheduleForRemoval(taskId, 10.seconds)(context.system)
  }
  
  def doWork(doc: DocumentRecord, part: DocumentFilepartRecord, dir: File, args: Map[String, String], taskId: UUID)
  
}

object WorkerActor {
  
  case class WorkOnPart(
    document : DocumentRecord,
    part     : DocumentFilepartRecord,
    dir      : File,
    args     : Map[String, String] = Map.empty[String, String]) 
  
}
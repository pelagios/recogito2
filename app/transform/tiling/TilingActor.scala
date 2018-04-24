package transform.tiling

import akka.actor.{Actor, Props}
import java.io.File
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.task.{TaskService, TaskStatus}

class TilingActor(taskService : TaskService) extends Actor {
  
  def receive = {
    
    case msg: TilingActor.ProcessImage =>      
      val filename = msg.part.getFile
      
      val tilesetDir =
        new File(msg.dir, filename.substring(0, filename.lastIndexOf('.')))
      
      val taskId = Await.result(
        taskService.insertTask(
          TilingService.TASK_TYPE,
          this.getClass.getName,
          Some(msg.document.getId),
          Some(msg.part.getId),
          Some(msg.document.getOwner)),
        10.seconds)
        
      taskService.updateStatusAndProgress(taskId, TaskStatus.RUNNING, 1)
      
      Try(TilingService.createZoomify(new File(msg.dir, filename), tilesetDir)) match {
        case Success(_) =>
          taskService.setCompleted(taskId)
          
        case Failure(t) =>
          taskService.setFailed(taskId, Some(t.getMessage))
      }
      
      taskService.scheduleForRemoval(taskId, 10.seconds)(context.system)
  }
  
}

object TilingActor {
  
  def props(taskService: TaskService) = Props(classOf[TilingActor], taskService)
    
  case class ProcessImage(
    document : DocumentRecord,
    part     : DocumentFilepartRecord,
    dir      : File,
    args     : Map[String, String]) 

}
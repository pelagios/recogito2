package transform.tiling

import akka.actor.Actor
import java.io.File
import models.task.{ TaskService, TaskStatus }
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._

private[tiling] class TilingWorkerActor(
    document: DocumentRecord,
    part: DocumentFilepartRecord,
    documentDir: File,
    taskService: TaskService,
    implicit val ctx: ExecutionContext) extends Actor {

  import transform.TransformTaskMessages._ 

  def receive = {

    case Start => {   
      val origSender = sender
      
      val filename = part.getFile
      val tilesetDir =
        new File(documentDir, filename.substring(0, filename.lastIndexOf('.')))
      
      val taskId = Await.result(
        taskService.insertTask(
          TilingService.TASK_TYPE,
          this.getClass.getName,
          Some(document.getId),
          Some(part.getId),
          Some(document.getOwner)),
        10.seconds)
        
      taskService.updateStatusAndProgress(taskId, TaskStatus.RUNNING, 1)
      
      TilingService
        .createZoomify(new File(documentDir, filename), tilesetDir).map(_ => {
          taskService.setCompleted(taskId)
          origSender ! Stopped
        }).recover { case t =>  {
          t.printStackTrace
          taskService.setFailed(taskId, Some(t.getMessage))
          origSender ! Stopped
        }}
    }

  }
  
}

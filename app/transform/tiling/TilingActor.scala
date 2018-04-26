package transform.tiling

import akka.actor.Props
import java.io.File
import java.util.UUID
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.task.TaskService
import transform.WorkerActor

class TilingActor(taskService: TaskService) extends WorkerActor(TilingService.TASK_TYPE, taskService) {

  def doWork(
    doc: DocumentRecord, 
    part: DocumentFilepartRecord, 
    dir: File, 
    args: Map[String, String], 
    taskId: UUID
  ) = {
    val filename = part.getFile
    val tilesetDir = new File(dir, filename.substring(0, filename.lastIndexOf('.')))
    
    try {
      TilingService.createZoomify(new File(dir, filename), tilesetDir)
      taskService.setCompleted(taskId)
    } catch { case t: Throwable =>
      taskService.setFailed(taskId, Some(t.getMessage))
    }    
  }
  
}

object TilingActor {
  
  def props(taskService: TaskService) = Props(classOf[TilingActor], taskService)

}
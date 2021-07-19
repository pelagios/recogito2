package transform.mapkurator

import akka.actor.Props
import java.io.File
import java.util.UUID
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.task.TaskService
import transform.{WorkerActor, SpecificJobDefinition}

class MapkuratorActor(taskService: TaskService) extends WorkerActor(MapkuratorService.TASK_TYPE, taskService) {

  override def doWork(
    doc: DocumentRecord, 
    part: DocumentFilepartRecord, 
    dir: File, 
    jobDef: Option[SpecificJobDefinition], 
    taskId: UUID
  ) = {
    val filename = part.getFile
    
    try {
      MapkuratorService.callMapkurator(new File(dir, filename))
      taskService.setTaskCompleted(taskId)
    } catch { case t: Throwable =>
      taskService.setTaskFailed(taskId, Some(t.getMessage))
    }    
  }
  
}

object MapkuratorActor {
  
  def props(taskService: TaskService) = Props(classOf[MapkuratorActor], taskService)

}
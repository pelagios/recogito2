package transform.mapkurator

import akka.actor.Props
import java.io.File
import java.util.UUID
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.ContentType
import services.task.TaskService
import transform.{WorkerActor, SpecificJobDefinition}

class MapKuratorActor(taskService: TaskService) extends WorkerActor(MapKuratorService.TASK_TYPE, taskService) {

  override def doWork(
    doc: DocumentRecord, 
    part: DocumentFilepartRecord, 
    dir: File, 
    jobDef: Option[SpecificJobDefinition], 
    taskId: UUID
  ) = {   
    try {
      MapKuratorService.callMapkurator(doc, part, dir, jobDef.get.asInstanceOf[MapKuratorJobDefinition])
      taskService.setTaskCompleted(taskId)
    } catch { case t: Throwable =>
      taskService.setTaskFailed(taskId, Some(t.getMessage))
    }    
  }
  
}

object MapKuratorActor {
  
  def props(taskService: TaskService) = Props(classOf[MapKuratorActor], taskService)

}
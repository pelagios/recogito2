package transform.tei

import akka.actor.Actor
import java.io.File
import models.generated.tables.records.DocumentRecord
import models.generated.tables.records.DocumentFilepartRecord
import models.task.{ TaskService, TaskStatus }
import scala.concurrent.{ Await, ExecutionContext } 
import scala.concurrent.duration._

private[tei] class TEIParserWorkerActor(
    document: DocumentRecord,
    part: DocumentFilepartRecord,
    documentDir: File,
    taskService: TaskService,
    implicit val ctx: ExecutionContext) extends Actor {
  
  import transform.TransformTaskMessages._
 
  def receive = {

    case Start => {
      val origSender = sender

      val taskId = Await.result(
        taskService.insertTask(
          TEIParserService.TASK_TYPE,
          this.getClass.getName,
          Some(document.getId),
          Some(part.getId),
          Some(document.getOwner)),
        10.seconds)
        
      taskService.updateStatusAndProgress(taskId, TaskStatus.RUNNING, 1)
      
      TEIParserService.extractEntities(new File(documentDir, part.getFile)).map { entities =>
        
        // TODO import annotations to index
        
        play.api.Logger.info("Completed.")
        entities.foreach { e => play.api.Logger.info(e.toString) }
        
        taskService.setCompleted(taskId)
        origSender ! Stopped
      } recover { case t: Throwable =>
        t.printStackTrace
        taskService.setFailed(taskId, Some(t.getMessage))
        origSender ! Stopped 
      }
    }

  }
  
}
package transform.tei

import akka.actor.Actor
import java.io.File
import services.annotation.AnnotationService
import services.generated.tables.records.DocumentRecord
import services.generated.tables.records.DocumentFilepartRecord
import services.task.{ TaskService, TaskStatus }
import scala.concurrent.{ Await, ExecutionContext } 
import scala.concurrent.duration._
import play.api.Logger

private[tei] class TEIParserWorkerActor(
    document: DocumentRecord,
    part: DocumentFilepartRecord,
    documentDir: File,
    annotationService: AnnotationService,
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
      
      TEIParserService.extractEntities(part, new File(documentDir, part.getFile))
        .flatMap(annotationService.upsertAnnotations(_)).map { failed =>
          if (failed.size == 0) {
            taskService.setCompleted(taskId)
          } else {
            val msg = "Failed to store " + failed.size + " annotations"
            Logger.warn(msg)
            failed.foreach(a => Logger.warn(a.toString))
            taskService.setFailed(taskId, Some(msg))
          }
            
          origSender ! Stopped
        } recover { case t: Throwable =>
          t.printStackTrace
          taskService.setFailed(taskId, Some(t.getMessage))
          origSender ! Stopped 
        }
    }

  }
  
}
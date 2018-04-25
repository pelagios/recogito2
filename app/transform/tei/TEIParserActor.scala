package transform.tei

import akka.actor.{Actor, Props}
import java.io.File
import play.api.Logger
import scala.concurrent.Await
import scala.concurrent.duration._
import services.annotation.AnnotationService
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.task.{TaskService, TaskStatus}

class TEIParserActor(
  taskService: TaskService,
  annotationService: AnnotationService
) extends Actor {
  
  implicit val ctx = context.dispatcher
  
  def receive = {
    
    case  msg: TEIParserActor.ProcessTEI =>
      val taskId = Await.result(
        taskService.insertTask(
          TEIParserService.TASK_TYPE,
          this.getClass.getName,
          Some(msg.document.getId),
          Some(msg.part.getId),
          Some(msg.document.getOwner)),
        10.seconds)
        
      taskService.updateStatusAndProgress(taskId, TaskStatus.RUNNING, 1)
      
      val annotations = TEIParserService.extractEntities(msg.part, new File(msg.dir, msg.part.getFile))
      
      val fUpsertAll = annotationService.upsertAnnotations(annotations).map { failed =>
        if (failed.size == 0) {
          taskService.setCompleted(taskId)
        } else {
          val msg = "Failed to store " + failed.size + " annotations"
          Logger.warn(msg)
          failed.foreach(a => Logger.warn(a.toString))
          taskService.setFailed(taskId, Some(msg))
        } 
      } recover { case t: Throwable =>
        t.printStackTrace
        taskService.setFailed(taskId, Some(t.getMessage))
      }

      Await.result(fUpsertAll, 20.minutes)
      taskService.scheduleForRemoval(taskId, 10.seconds)(context.system)
  }
  
}

object TEIParserActor {
  
  def props(taskService: TaskService, annotationService: AnnotationService) =
    Props(classOf[TEIParserActor], taskService, annotationService)
    
  case class ProcessTEI(
    document : DocumentRecord,
    part     : DocumentFilepartRecord,
    dir      : File) 
  
}
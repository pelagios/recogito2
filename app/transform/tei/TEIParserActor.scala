package transform.tei

import akka.actor.Props
import java.io.File
import java.util.UUID
import play.api.Logger
import scala.concurrent.Await
import scala.concurrent.duration._
import services.annotation.AnnotationService
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.task.TaskService
import transform.WorkerActor

class TEIParserActor(
  taskService: TaskService,
  annotationService: AnnotationService
) extends WorkerActor(TEIParserService.TASK_TYPE, taskService) {
  
  implicit val ctx = context.dispatcher
  
  override def doWork(doc: DocumentRecord, part: DocumentFilepartRecord, dir: File, args: Map[String, String], taskId: UUID) = {
    val annotations = TEIParserService.extractEntities(part, new File(dir, part.getFile))
    
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
  }
  
}

object TEIParserActor {
  
  def props(taskService: TaskService, annotationService: AnnotationService) =
    Props(classOf[TEIParserActor], taskService, annotationService)
  
}
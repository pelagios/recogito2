package transform.georesolution

import akka.actor.{ ActorSystem, Props }
import java.io.File
import javax.inject.{ Inject, Singleton }
import services.annotation.AnnotationService
import services.place.PlaceService
import services.task.{ TaskService, TaskType }
import services.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import storage.Uploads
import transform._

object GeoresolutionService {
  
  val TASK_TYPE = TaskType("GEORESOLUTION")

}

@Singleton
class GeoresolutionService @Inject() (
    annotations: AnnotationService,
    places: PlaceService,
    taskService: TaskService,
    uploads: Uploads,
    ctx: ExecutionContext) extends TransformService {
  
  override def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], args: Map[String, String])(implicit system: ActorSystem): Unit =
    spawnTask(document, parts, uploads.getDocumentDir(document.getOwner, document.getId).get, args, 10.minutes)

  /** We're splitting this, so we can inject alternative folders for testing **/
  private[georesolution] def spawnTask(
      document: DocumentRecord,
      parts: Seq[DocumentFilepartRecord],
      sourceFolder: File,
      args: Map[String, String],
      keepalive: FiniteDuration)(implicit system: ActorSystem): Unit = {
    
    val actor = system.actorOf(
        Props(
          classOf[GeoresolutionSupervisorActor], 
          document, 
          parts,
          sourceFolder,
          args,
          taskService,
          annotations,
          places,
          keepalive,
          ctx),
        name = "georesolution.doc." + document.getId)
        
    actor ! TransformTaskMessages.Start
  }
  
}
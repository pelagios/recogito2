package transform.georesolution

import akka.actor.{ ActorSystem, Props }
import java.io.File
import javax.inject.{ Inject, Singleton }
import models.annotation.AnnotationService
import models.place.PlaceService
import models.task.{ TaskService, TaskType }
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import storage.Uploads
import transform._
import com.vividsolutions.jts.geom.Point

case class Georesolvable(toponym: String, coord: Option[Point])

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
  
  override def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord])(implicit system: ActorSystem): Unit =
    spawnTask(document, parts, uploads.getDocumentDir(document.getOwner, document.getId).get)

  /** We're splitting this, so we can inject alternative folders for testing **/
  private[georesolution] def spawnTask(
      document: DocumentRecord,
      parts: Seq[DocumentFilepartRecord],
      sourceFolder: File,
      keepalive: FiniteDuration = 10.minutes)(implicit system: ActorSystem): Unit = {
    
    val actor = system.actorOf(
        Props(
          classOf[GeoresolutionSupervisorActor], 
          document, 
          parts,
          sourceFolder,
          taskService,
          annotations,
          places,
          keepalive,
          ctx),
        name = "georesolution.doc." + document.getId)
        
    actor ! TransformTaskMessages.Start
  }
  
}
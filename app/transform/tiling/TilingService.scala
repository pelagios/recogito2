package transform.tiling

import akka.actor.{ ActorSystem, Props }
import java.io.File
import javax.inject.{ Inject, Singleton }
import models.task.{ TaskService, TaskType }
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.{ Future, ExecutionContext }
import scala.concurrent.duration._
import scala.language.postfixOps
import storage.Uploads
import sys.process._
import transform._

object TilingService {

  val TASK_TYPE = TaskType("IMAGE_TILING")
  
  private[tiling] def createZoomify(file: File, destFolder: File)(implicit context: ExecutionContext): Future[Unit] = {
    Future {
      s"vips dzsave $file $destFolder --layout zoomify" !
    } map { result =>
      if (result == 0)
        Unit
      else
        throw new Exception("Image tiling failed for " + file.getAbsolutePath + " to " + destFolder.getAbsolutePath)
    }
  }
  
}

@Singleton
class TilingService @Inject() (uploads: Uploads, taskService: TaskService, ctx: ExecutionContext) extends TransformService {

  override def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], args: Map[String, String])(implicit system: ActorSystem): Unit =
    spawnTask(document, parts, uploads.getDocumentDir(document.getOwner, document.getId).get, args, 10.minutes)

  /** We're splitting this, so we can inject alternative folders for testing **/
  private[tiling] def spawnTask(
      document: DocumentRecord,
      parts: Seq[DocumentFilepartRecord],
      sourceFolder: File,
      args: Map[String, String],
      keepalive: FiniteDuration)(implicit system: ActorSystem): Unit = {
    
    val actor = system.actorOf(
      Props(
        classOf[TilingSupervisorActor],
        document,
        parts,
        sourceFolder,
        args,
        taskService,
        keepalive,
        ctx),
      name = "tile.doc." + document.getId)
      
    actor ! TransformTaskMessages.Start
  }

}

package controllers.my.upload.tiling

import akka.actor.{ ActorSystem, Props }
import controllers.my.upload._
import java.io.File
import javax.inject.{ Inject, Singleton }
import models.task.TaskService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import storage.Uploads
import sys.process._

object TilingService {
  
  val TASK_TILING = TaskType("IMAGE_TILING")

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
class TilingService @Inject() (uploads: Uploads, taskService: TaskService) extends ProcessingService {

  /** Spawns a new background tiling process.
    *
    * The function will throw an exception in case the user data directory
    * for any of the fileparts does not exist. This should, however, never
    * happen. If it does, something is seriously broken with the DB integrity.
    */
  override def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord])(implicit system: ActorSystem): Unit =
    spawnTask(document, parts, uploads.getDocumentDir(document.getOwner, document.getId).get)

  /** We're splitting this function, so we can inject alternative folders for testing **/
  private[tiling] def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], sourceFolder: File)(implicit system: ActorSystem): Unit = {
    val actor = system.actorOf(Props(classOf[TilingSupervisorActor], TilingService.TASK_TILING, document, parts, sourceFolder, taskService), name = "tile_doc_" + document.getId)
    actor ! ProcessingMessages.Start
  }

}

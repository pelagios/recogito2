package transform.tiling

import akka.actor.ActorSystem
import akka.routing.RoundRobinPool
import java.io.File
import javax.inject.{Inject, Singleton}
import scala.language.postfixOps
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.task.{TaskService, TaskType}
import storage.uploads.Uploads
import sys.process._
import transform.{WorkerActor, WorkerService}

@Singleton
class TilingService @Inject() (
  uploads: Uploads,
  taskService: TaskService, 
  system: ActorSystem
) extends WorkerService(
  system, uploads,
  TilingActor.props(taskService), 4
)      

object TilingService {

  val TASK_TYPE = TaskType("IMAGE_TILING")
  
  private[tiling] def createZoomify(file: File, destFolder: File) = {
    
    val result =  s"vips dzsave $file $destFolder --layout zoomify" !
    
    if (result != 0)
      throw new Exception("Image tiling failed for " + file.getAbsolutePath + " to " + destFolder.getAbsolutePath)
  }
  
}
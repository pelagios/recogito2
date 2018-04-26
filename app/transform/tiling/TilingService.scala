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
import transform.WorkerActor

@Singleton
class TilingService @Inject() (
  uploads: Uploads,
  taskService: TaskService, 
  system: ActorSystem
) {
  
  val routerProps = 
    TilingActor.props(taskService)
      .withRouter(RoundRobinPool(nrOfInstances = 4))
      .withDispatcher("contexts.background-workers")
      
  val router = system.actorOf(routerProps)

  def spawnTask(
    document: DocumentRecord,
    parts   : Seq[DocumentFilepartRecord]
  ) = parts.foreach { part =>  
    router ! WorkerActor.WorkOnPart(
      document,
      part,
      uploads.getDocumentDir(document.getOwner, document.getId).get)
  }

}

object TilingService {

  val TASK_TYPE = TaskType("IMAGE_TILING")
  
  private[tiling] def createZoomify(file: File, destFolder: File) = {
    
    val result =  s"vips dzsave $file $destFolder --layout zoomify" !
    
    if (result != 0)
      throw new Exception("Image tiling failed for " + file.getAbsolutePath + " to " + destFolder.getAbsolutePath)
  }
  
}
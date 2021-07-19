package transform.mapkurator

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
class MapkuratorService @Inject() (
  uploads: Uploads,
  taskService: TaskService, 
  system: ActorSystem
) extends WorkerService(
  system, uploads,
  MapkuratorActor.props(taskService), 4
)      

object MapkuratorService {

  val TASK_TYPE = TaskType("MAPKURATOR")
  
  private[mapkurator] def callMapkurator(file: File) = {

    play.api.Logger.info("This is where the callout to mapKurator will happen!") 
    play.api.Logger.info(file.toString)   

  }
  
}
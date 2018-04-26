package transform.georesolution

import akka.actor.ActorSystem
import akka.routing.RoundRobinPool
import javax.inject.{Inject, Singleton}
import services.annotation.AnnotationService
import services.entity.builtin.EntityService
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.task.{TaskService, TaskType}
import storage.uploads.Uploads
import transform.WorkerActor

@Singleton
class GeoresolutionService @Inject() (
  annotationService: AnnotationService,
  entityService: EntityService,
  taskService: TaskService,
  uploads: Uploads,
  system: ActorSystem
) {

  val routerProps = 
    GeoresolutionActor.props(taskService, annotationService, entityService)
      .withRouter(RoundRobinPool(nrOfInstances = 2))
      
  val router = system.actorOf(routerProps)

  def spawnTask(
    document: DocumentRecord,
    parts   : Seq[DocumentFilepartRecord],
    args    : Map[String, String]
  ) = parts.foreach { part =>  
    router ! WorkerActor.WorkOnPart(
      document,
      part,
      uploads.getDocumentDir(document.getOwner, document.getId).get,
      args)
  }
  
}

object GeoresolutionService {
  
  val TASK_TYPE = TaskType("GEORESOLUTION")

}

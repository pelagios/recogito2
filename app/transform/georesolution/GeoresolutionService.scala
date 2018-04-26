package transform.georesolution

import akka.actor.ActorSystem
import akka.routing.RoundRobinPool
import javax.inject.{Inject, Singleton}
import services.annotation.AnnotationService
import services.entity.builtin.EntityService
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.task.{TaskService, TaskType}
import storage.uploads.Uploads
import transform.{WorkerActor, WorkerService}

@Singleton
class GeoresolutionService @Inject() (
  annotationService: AnnotationService,
  entityService: EntityService,
  taskService: TaskService,
  uploads: Uploads,
  system: ActorSystem
) extends WorkerService (
  system, uploads,
  GeoresolutionActor.props(taskService, annotationService, entityService), 4
)

object GeoresolutionService {
  
  val TASK_TYPE = TaskType("GEORESOLUTION")

}

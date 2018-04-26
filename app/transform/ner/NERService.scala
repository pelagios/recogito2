package transform.ner

import akka.actor.ActorSystem
import akka.routing.RoundRobinPool
import javax.inject.{Inject, Singleton}
import org.pelagios.recogito.sdk.ner.Entity
import scala.collection.JavaConverters._
import services.annotation.AnnotationService
import services.entity.builtin.EntityService
import services.task.{TaskService, TaskType}
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import storage.uploads.Uploads
import transform.{WorkerActor, WorkerService}

@Singleton
class NERService @Inject() (
  annotationService: AnnotationService,
  entityService: EntityService,
  taskService: TaskService,
  uploads: Uploads,
  system: ActorSystem
) extends WorkerService(
  system, uploads,
  NERActor.props(taskService, annotationService, entityService), 10  
)

object NERService {

  val TASK_TYPE = TaskType("NER")
  
  private[ner] def parse(text: String): Seq[Entity] = {
    val ner = NERPluginManager.getDefaultNER
    val entities = ner.parse(text)
    entities.asScala
  }
    
}
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
import transform.WorkerActor

@Singleton
class NERService @Inject() (
  annotationService: AnnotationService,
  entityService: EntityService,
  taskService: TaskService,
  uploads: Uploads,
  system: ActorSystem
) {
  
  val routerProps = 
    NERActor.props(taskService, annotationService, entityService)
      .withRouter(RoundRobinPool(nrOfInstances = 10))
      
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

object NERService {

  val TASK_TYPE = TaskType("NER")
  
  private[ner] def parse(text: String): Seq[Entity] = {
    val ner = NERPluginManager.getDefaultNER
    val entities = ner.parse(text)
    entities.asScala
  }
    
}
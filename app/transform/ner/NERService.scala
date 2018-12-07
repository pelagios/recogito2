package transform.ner

import akka.actor.ActorSystem
import akka.routing.RoundRobinPool
import javax.inject.{Inject, Singleton}
import org.pelagios.recogito.sdk.ner.Entity
import scala.collection.JavaConverters._
import services.annotation.AnnotationService
import services.entity.builtin.EntityService
import services.task.{TaskService, TaskType}
import storage.uploads.Uploads
import transform.WorkerService

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

object NERService extends HasTeiNER {

  val TASK_TYPE = TaskType("NER")
  
  /** Parses the text and returns the NER results as a list of entities **/
  private[ner] def parseText(text: String, engine: Option[String]): Seq[Entity] = {
    val ner = engine match {
      case Some(identifier) => NERPluginManager.getEngine(identifier).get
      case None => NERPluginManager.getDefaultEngine
    }

    val entities = ner.parse(text)
    entities.asScala
  }
     
}
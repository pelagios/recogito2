package transform.ner

import akka.actor.ActorSystem
import akka.routing.RoundRobinPool
import javax.inject.{Inject, Singleton}
import org.pelagios.recogito.sdk.PluginEnvironment
import org.pelagios.recogito.sdk.ner.Entity
import play.api.{Configuration, Logger}
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
  config: Configuration,
  system: ActorSystem
) extends WorkerService(
  system, uploads,
  NERActor.props(taskService, annotationService, entityService, config), 10  
)

object NERService extends HasTeiNER {

  val TASK_TYPE = TaskType("NER")

  private def getEnvironment(config: Configuration) = {
    // Not the nicest way to handle this - suggestions welcome!
    val userDir = if (System.getProperty("user.dir").contains("/target/universal/stage"))
      System.getProperty("user.dir").replace("/target/universal/stage", "")
    else 
      System.getProperty("user.dir")

    val pluginsDir = s"${userDir}/plugins"

    val asMap = config.entrySet
      .filter(_._1.startsWith("plugins"))
      .toSeq
      .map { case(key, value) =>
        (key -> value.unwrapped.toString)
      }.toMap.asJava

    new PluginEnvironment(pluginsDir, asMap)
  }
  
  /** Parses the text and returns the NER results as a list of entities **/
  private[ner] def parseText(text: String, engine: Option[String], config: Configuration): Seq[Entity] = {
    val ner = engine match {
      case Some(identifier) => NERPluginManager.getEngine(identifier).get
      case None => NERPluginManager.getDefaultEngine
    }

    Logger.info(s"Running NER with engine ${ner.getName}")

    val entities = ner.parse(text, getEnvironment(config))
    entities.asScala
  }
     
}
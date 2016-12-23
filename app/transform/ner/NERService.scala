package transform.ner

import akka.actor.{ ActorSystem, Props }
import java.io.File
import javax.inject.{ Inject, Singleton }
import models.ContentType
import models.annotation.AnnotationService
import models.place.PlaceService
import models.task.{ TaskService, TaskType }
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.pelagios.recogito.sdk.ner._
import play.api.Logger
import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import storage.Uploads
import transform._

object NERService { 
  
  val TASK_TYPE = TaskType("NER")
  
  val SUPPORTED_CONTENT_TYPES = Set(ContentType.TEXT_PLAIN).map(_.toString)

  private var runningPipelines = 0

  private[ner] def parse(text: String)(implicit context: ExecutionContext): Future[Seq[Entity]] = {
    runningPipelines += 1
    
    if (runningPipelines > 5)
      Logger.warn(runningPipelines + " runnning NER pipelines")
    
    Future {
      scala.concurrent.blocking {
        // TODO to be extended in the future
        val ner = NERPluginManager.getDefaultNER
        val entities = ner.parse(text)
    
        runningPipelines -= 1

        entities.asScala
      }
    }
  }
  
}

@Singleton
class NERService @Inject() (
    annotations: AnnotationService,
    places: PlaceService,
    taskService: TaskService,
    uploads: Uploads,
    ctx: ExecutionContext) extends TransformService {

  /** Spawns a new background parse process.
    *
    * The function will throw an exception in case the user data directory
    * for any of the fileparts does not exist. This should, however, never
    * happen. If it does, something is seriously broken with the DB integrity.
    */
  override def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], args: Map[String, String])(implicit system: ActorSystem): Unit =
    spawnTask(document, parts, uploads.getDocumentDir(document.getOwner, document.getId).get, args, 10.minutes)

  /** We're splitting this, so we can inject alternative folders for testing **/
  private[ner] def spawnTask(
      document: DocumentRecord,
      parts: Seq[DocumentFilepartRecord],
      sourceFolder: File,
      args: Map[String, String],
      keepalive: FiniteDuration)(implicit system: ActorSystem): Unit = {
    
    val actor = system.actorOf(
        Props(
          classOf[NERSupervisorActor], 
          document, 
          parts,
          sourceFolder,
          args,
          taskService,
          annotations,
          places,
          keepalive,
          ctx),
        name = "ner.doc." + document.getId)
        
    actor ! TransformTaskMessages.Start
  }

}

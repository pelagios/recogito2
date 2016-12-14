package controllers.my.upload.ner

import akka.actor.{ ActorSystem, Props }
import akka.pattern.ask
import akka.util.Timeout
import controllers.my.upload._
import java.io.File
import javax.inject.{ Inject, Singleton }
import models.annotation.AnnotationService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import models.place.PlaceService
import models.task.TaskService
import org.pelagios.recogito.sdk.ner.Entity
import play.api.Logger
import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import storage.Uploads

object NERService { 
  
  val TASK_NER = TaskType("NER")

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
class NERService @Inject() (annotations: AnnotationService, places: PlaceService, taskService: TaskService, uploads: Uploads) extends ProcessingService {

  /** Spawns a new background parse process.
    *
    * The function will throw an exception in case the user data directory
    * for any of the fileparts does not exist. This should, however, never
    * happen. If it does, something is seriously broken with the DB integrity.
    */
  override def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord])(implicit system: ActorSystem): Unit =
    spawnTask(document, parts, uploads.getDocumentDir(document.getOwner, document.getId).get)

  /** We're splitting this function, so we can inject alternative folders for testing **/
  private[ner] def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], sourceFolder: File, keepalive: Duration = 10.minutes)(implicit system: ActorSystem): Unit = {
    val actor = system.actorOf(Props(classOf[NERSupervisorActor], NERService.TASK_NER, document, parts, sourceFolder, keepalive, taskService, annotations, places), name = "ner_doc_" + document.getId)
    actor ! ProcessingTaskMessages.Start
  }

  /** Queries the progress for a specific process **/
  override def queryProgress(documentId: String, timeout: FiniteDuration = 60.seconds)(implicit context: ExecutionContext, system: ActorSystem) = {
    ProcessingTaskSupervisor.getSupervisorActor(NERService.TASK_NER, documentId) match {
      case Some(actor) => {
        implicit val t = Timeout(timeout)
        (actor ? ProcessingTaskMessages.QueryProgress).mapTo[ProcessingTaskMessages.DocumentProgress].map(Some(_))
      }

      case None =>
        Future.successful(None)
    }
  }

}

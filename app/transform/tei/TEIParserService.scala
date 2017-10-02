package transform.tei

import akka.actor.{ ActorSystem, Props }
import java.io.File
import javax.inject.{ Inject, Singleton }
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import models.task.{ TaskType, TaskService }
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import storage.Uploads
import transform.{ TransformService, TransformTaskMessages }

case class TEIEntity()

object TEIParserService {
  
  val TASK_TYPE = TaskType("TEI_PARSING")
  
  def extractEntities(file: File): Future[Seq[TEIEntity]] = ???
  
}

@Singleton
class TEIParserService @Inject() (uploads: Uploads, taskService: TaskService, ctx: ExecutionContext) extends TransformService {

  override def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], args: Map[String, String])(implicit system: ActorSystem): Unit =
    spawnTask(document, parts, uploads.getDocumentDir(document.getOwner, document.getId).get, 10.minutes)
  
  private[tei] def spawnTask(
      document: DocumentRecord,
      parts: Seq[DocumentFilepartRecord],
      sourceFolder: File,
      keepalive: FiniteDuration)(implicit system: ActorSystem): Unit = {
    
    val actor = system.actorOf(
      Props(
        classOf[TEIParserSupervisorActor],
        document,
        parts,
        sourceFolder,
        taskService,
        keepalive,
        ctx),
      name = "tei.doc." + document.getId)
      
    actor ! TransformTaskMessages.Start
  }
  
}
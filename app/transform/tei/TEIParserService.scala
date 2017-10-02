package transform.tei

import akka.actor.{ ActorSystem, Props }
import java.io.File
import javax.inject.{ Inject, Singleton }
import models.annotation.AnnotationBody
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import models.task.{ TaskType, TaskService }
import org.joox.JOOX._
import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.xml.XML
import storage.Uploads
import transform.{ TransformService, TransformTaskMessages }
import java.io.FileInputStream

case class TEIEntity(entityType: AnnotationBody.Type, quote: String, xpath: String)

object TEIParserService {
  
  val TASK_TYPE = TaskType("TEI_PARSING")
  
  def extractEntities(file: File)(implicit ctx: ExecutionContext): Future[Seq[TEIEntity]] = Future {
    val teiBefore = $(file).document()
    
    // JOOX uses mutable data structures - create a working copy
    val teiAfter = $(file).document()
    
    val places = $(teiAfter).find("placeName").get.map { el =>
      val text = el.getTextContent
      val xpath = $(el).xpath
      
      $(el).replaceWith(text)
      
      TEIEntity(AnnotationBody.PLACE, text, xpath)
    }
    
    // TODO repeat for personName
    play.api.Logger.info($(teiAfter).toString)
    
      
    places
  }
  
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
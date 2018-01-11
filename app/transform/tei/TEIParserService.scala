package transform.tei

import akka.actor.{ ActorSystem, Props }
import java.io.{ File, PrintWriter }
import javax.inject.{ Inject, Singleton }
import services.annotation.{ Annotation, AnnotationService }
import services.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import services.task.{ TaskType, TaskService }
import org.joox.JOOX._
import org.w3c.dom.ranges.DocumentRange
import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import storage.Uploads
import transform.{ TransformService, TransformTaskMessages }

object TEIParserService {

  val TASK_TYPE = TaskType("TEI_PARSING")

  def extractEntities(
      part: DocumentFilepartRecord,
      file: File,
      replaceOriginalFile: Boolean = true
  )(implicit ctx: ExecutionContext): Future[Seq[Annotation]] = Future {
        
    val teiXML = $(file).document()
    val ranges = teiXML.asInstanceOf[DocumentRange]

    val places = $(teiXML).find("placeName").get
    val people = $(teiXML).find("persName").get
    val spans  = $(teiXML).find("span").get
        
    val annotations = (places ++ people ++ spans).map(TEITag.convert(part, _, ranges))

    if (replaceOriginalFile)
      new PrintWriter(file.getAbsolutePath)  {
        write($(teiXML).toString)
        close
      }

    annotations
  }

}

@Singleton
class TEIParserService @Inject() (uploads: Uploads, annotationService: AnnotationService, taskService: TaskService, ctx: ExecutionContext) extends TransformService {

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
        annotationService,
        taskService,
        keepalive,
        ctx),
      name = "tei.doc." + document.getId)

    actor ! TransformTaskMessages.Start
  }

}

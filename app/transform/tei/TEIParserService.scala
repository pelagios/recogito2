package transform.tei

import akka.actor.ActorSystem
import akka.routing.RoundRobinPool
import java.io.{File, PrintWriter}
import javax.inject.{Inject, Singleton}
import org.joox.JOOX._
import org.w3c.dom.ranges.DocumentRange
import scala.collection.JavaConversions._
import services.annotation.{Annotation, AnnotationService}
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.task.{TaskService, TaskType}
import storage.uploads.Uploads
import transform.{WorkerActor, WorkerService}

@Singleton
class TEIParserService @Inject() (
  uploads: Uploads,
  annotationService: AnnotationService,
  taskService: TaskService, 
  system: ActorSystem
) extends WorkerService(
  system, uploads,
  TEIParserActor.props(taskService, annotationService), 10
)

object TEIParserService {

  val TASK_TYPE = TaskType("TEI_PARSING")
  
  private[tei] def extractEntities(
    part: DocumentFilepartRecord,
    file: File,
    replaceOriginalFile: Boolean = true
  ): Seq[Annotation] = {    
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
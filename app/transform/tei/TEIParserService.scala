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

@Singleton
class TEIParserService @Inject() (
  uploads: Uploads,
  annotationService: AnnotationService,
  taskService: TaskService, 
  system: ActorSystem
) {
  
  val routerProps = 
    TEIParserActor.props(taskService, annotationService)
      .withRouter(RoundRobinPool(nrOfInstances = 10))
      
  val router = system.actorOf(routerProps)

  def spawnTask(
    document: DocumentRecord,
    parts   : Seq[DocumentFilepartRecord]
  ) = parts.foreach { part =>  
    router ! TEIParserActor.ProcessTEI(
      document,
      part,
      uploads.getDocumentDir(document.getOwner, document.getId).get)
  }

}

object TEIParserService {

  val TASK_TYPE = TaskType("TEI_PARSING")
  
  private[tei] def extractEntities(
    part: DocumentFilepartRecord,
    file: File,
    replaceOriginalFile: Boolean = true
  ) = {    
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
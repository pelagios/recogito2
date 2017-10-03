package transform.tei

import akka.actor.{ ActorSystem, Props }
import java.io.File
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import models.annotation._
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import models.task.{ TaskType, TaskService }
import org.joda.time.DateTime
import org.joox.JOOX._
import org.w3c.dom.Node
import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.xml.XML
import storage.Uploads
import transform.{ TransformService, TransformTaskMessages }
import java.io.FileInputStream
import java.io.PrintWriter
import models.ContentType

object TEIParserService {
  
  val TASK_TYPE = TaskType("TEI_PARSING")
  
  private def toAnnotation(part: DocumentFilepartRecord, entityType: AnnotationBody.Type, quote: String, ref: Option[String], anchor: String) = {
    val now = DateTime.now
    
    Annotation(
      UUID.randomUUID,
      UUID.randomUUID,     
      AnnotatedObject(
        part.getDocumentId,
        part.getId,
        ContentType.TEXT_TEIXML),
      Seq.empty[String], // contributors    
      anchor,
      None, // lastModifiedBy
      now, // lastModifiedAt
      Seq(
        AnnotationBody(
          AnnotationBody.QUOTE,
          None,  // lastModifiedBy
          now,   // lastModifiedAt
          Some(quote),
          None,  // uri
          None), // status
          
        AnnotationBody(
          entityType,
          None, // lastModifiedBy
          now,  // lastModifiedAt
          None, // value
          ref,
          Some(AnnotationStatus(
            AnnotationStatus.VERIFIED,
            None,   // setBy
            now))) // setAt          
      )
    )
  }
  
  def extractEntities(part: DocumentFilepartRecord, file: File, replaceOriginalFile: Boolean = true)(implicit ctx: ExecutionContext): Future[Seq[Annotation]] = Future {
    
    def toAnchor(xpath: String, fromOffset: Int, toOffset: Int) = {
      val path = 
        xpath.substring(0, xpath.lastIndexOf('/')).toLowerCase
          .replaceAll("\\[1\\]", "") // Selecting first is redundant (and browser clients don't do it)
        
      "from=" + path + "::" + fromOffset + ";to=" + path + "::" + toOffset 
    }
    
    // JOOX uses mutable data structures - create a working copy
    val teiXML = $(file).document()
    
    val places = $(teiXML).find("placeName").get.map { el =>
      val xpath = $(el).xpath
      val text = el.getTextContent
      val offset = el.getPreviousSibling match { 
        case n if n.getNodeType == Node.TEXT_NODE => n.getTextContent.size
        case _ => 0
      }
      val anchor = toAnchor(xpath, offset, offset + text.size)
      
      val ref = el.getAttribute("ref") match {
        case s if s.isEmpty => None
        case s => Some(s)
      }

      // Convert to standoff annotation and remove from XML DOM
      $(el).replaceWith(text)   
      toAnnotation(part, AnnotationBody.PLACE, text, ref, anchor)
    }
    
    if (replaceOriginalFile)
      new PrintWriter(file.getAbsolutePath)  { 
        write($(teiXML).toString)
        close
      }
   
    places
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
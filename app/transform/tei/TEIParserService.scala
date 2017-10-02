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
import java.io.PrintWriter
import org.w3c.dom.Node

case class TEIEntity(entityType: AnnotationBody.Type, quote: String, ref: Option[String], anchor: String)

object TEIParserService {
  
  val TASK_TYPE = TaskType("TEI_PARSING")
  
  /** XPath will have a form like:
    *
    *    /TEI[1]/text[1]/body[1]/div[1]/p[1]/placeName[1]
    *
    * We need to remove the last segment (because it will be removed from the
    * DOM) and add the character offset.
    *  
    */
  private[tei] def toAnchor(xpath: String, fromOffset: Int, toOffset: Int) = {
    val path = xpath.substring(0, xpath.lastIndexOf('/')).toLowerCase
    
    // TODO just a temporary hack
    "from=" + path + "::" + fromOffset + ";to=" + path + "::" + toOffset 
  }
  
  def extractEntities(file: File, replaceOriginalFile: Boolean = true)(implicit ctx: ExecutionContext): Future[Seq[TEIEntity]] = Future {
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
      TEIEntity(AnnotationBody.PLACE, text, ref, anchor)  
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
package transform.tei

import akka.actor.{ ActorSystem, Props }
import java.io.{ File, PrintWriter }
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import models.ContentType
import models.annotation._
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import models.task.{ TaskType, TaskService }
import org.joda.time.DateTime
import org.joox.JOOX._
import org.w3c.dom.{ Element, Node }
import org.w3c.dom.ranges.DocumentRange
import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.xml.XML
import storage.Uploads
import transform.{ TransformService, TransformTaskMessages }

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
          None,  // note
          None), // status

        AnnotationBody(
          entityType,
          None, // lastModifiedBy
          now,  // lastModifiedAt
          None, // value
          ref,
          None, // note
          Some(AnnotationStatus(
            AnnotationStatus.VERIFIED,
            None,   // setBy
            now))) // setAt
      )
    )
  }

  def extractEntities(part: DocumentFilepartRecord, file: File, replaceOriginalFile: Boolean = true)(implicit ctx: ExecutionContext): Future[Seq[Annotation]] = Future {
    val teiXML = $(file).document()
    val ranges = teiXML.asInstanceOf[DocumentRange]

    def toAnchor(xpath: String, fromOffset: Int, toOffset: Int) = {
      val path =
        xpath.substring(0, xpath.lastIndexOf('/')).toLowerCase
          .replaceAll("\\[1\\]", "") // Selecting first is redundant (and browser clients don't do it)

      "from=" + path + "::" + fromOffset + ";to=" + path + "::" + toOffset
    }

    def convertEntity(entityType: AnnotationBody.Type, el: Element) = {
      val rangeBefore = ranges.createRange()
      rangeBefore.setStart(el.getParentNode, 0)
      rangeBefore.setEnd(el, 0)

      val offset = rangeBefore.toString.size
      val quote = el.getTextContent
      val xpath = $(el).xpath
      val anchor = toAnchor(xpath, offset, offset + quote.size)

      val ref = el.getAttribute("ref") match {
        case s if s.isEmpty => None
        case s => Some(s)
      }

      // Convert to standoff annotation and remove from XML DOM
      $(el).replaceWith(quote)
      toAnnotation(part, entityType, quote, ref, anchor)
    }

    val places = $(teiXML).find("placeName").get.map(convertEntity(AnnotationBody.PLACE, _))
    val people = $(teiXML).find("persName").get.map(convertEntity(AnnotationBody.PERSON, _))

    if (replaceOriginalFile)
      new PrintWriter(file.getAbsolutePath)  {
        write($(teiXML).toString)
        close
      }

    places ++ people
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

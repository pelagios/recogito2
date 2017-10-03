package controllers.document.downloads.serializers.tei

import controllers.document.downloads.serializers.BaseSerializer
import models.annotation.{ Annotation, AnnotationService }
import models.document.{ DocumentInfo, DocumentService }
import models.generated.tables.records.DocumentFilepartRecord
import org.w3c.dom.ranges.DocumentRange
import scala.concurrent.{ ExecutionContext, Future }
import scala.xml.Elem
import storage.Uploads
import javax.xml.parsers.DocumentBuilderFactory
import org.joox.JOOX._
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.xpath.XPathFactory
import javax.xml.xpath.XPathConstants
import org.w3c.dom.NodeList
import org.w3c.dom.Text
import models.annotation.AnnotationBody
import org.w3c.dom.Document

trait TEISerializer extends BaseSerializer {
  
  private val ENTITY_TYPES = Set(AnnotationBody.PLACE, AnnotationBody.PERSON)

  /** Not really a sort, at least not within the DOM. But sort only matter only within 
    * each tag individually. Therefore, it's sufficient if we naively sort everything 
    * by start offset. 
    */
  private def sortByOffsetDesc(annotations: Seq[Annotation]): Seq[Annotation] = annotations.sortBy { annotation =>
    val a = annotation.anchor
    val startOffset = a.substring(a.indexOf("::") + 2, a.indexOf(";")).toInt
    -startOffset
  }
  
  def partToTEI(part: DocumentFilepartRecord, xml: String, annotations: Seq[Annotation]) = {
    
    // TODO replace this with JOOX-based parsing (much shorter)
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val in = new InputSource(new StringReader(xml))
    val doc = builder.parse(in)
    val ranges = doc.asInstanceOf[DocumentRange]
    
    def separate(a: String): (String, Int) = {
      val path = a.substring(0, a.indexOf("::")).replaceAll("tei", "TEI")
      val offset = a.substring(a.indexOf("::") + 2).toInt
      (path, offset)
    }
    
    def toTag(annotation: Annotation) = {
      val quote = annotation.bodies.find(_.hasType == AnnotationBody.QUOTE).get.value.get
      val entityBody = annotation.bodies.find(b => ENTITY_TYPES.contains(b.hasType))
      val entityURI = entityBody.flatMap(_.uri)
      
      val el = entityBody match {
        case Some(b) if b.hasType == AnnotationBody.PLACE => doc.createElement("placeName")
        case Some(b) if b.hasType == AnnotationBody.PERSON => doc.createElement("personName")
        case _ => doc.createElement("span")
      }
      
      if (entityURI.isDefined) el.setAttribute("ref", entityURI.get)      
      el.appendChild(doc.createTextNode(quote))    
      
      el
    }
    
    sortByOffsetDesc(annotations).foreach { annotation =>
      val a = annotation.anchor
      
      val (startPath, startOffset) = separate(a.substring(5, a.indexOf(";")))
      val startNode = $(doc).xpath(startPath).get(0).getFirstChild
      
      val (endPath, endOffset) = separate(a.substring(a.indexOf(";") + 4))
      val endNode = $(doc).xpath(endPath).get(0).getFirstChild

      // We only support TEI export for annotations that don't cross node boundaries
      if (startNode == endNode) {
        val range = ranges.createRange()
        range.setStart(startNode, startOffset)
        range.setEnd(endNode, endOffset)
              
        val tag = toTag(annotation)
  
        range.deleteContents()
        range.surroundContents(tag)
      }
    }
    
    $(doc).toString
  }
  
  def teiToTEI(docInfo: DocumentInfo)(implicit documentService: DocumentService,
      uploads: Uploads, annotationService: AnnotationService, ctx: ExecutionContext): Future[Elem] = {
    
    val fParts = Future.sequence(
      docInfo.fileparts.map { part =>
        uploads.readTextfile(docInfo.owner.getUsername, docInfo.id, part.getFile).map { maybeText =>
          // If maybetext is None, integrity is broken -> let's fail
          (part, maybeText.get)
        }
      })
      
    val fAnnotationsByPart = 
      annotationService.findByDocId(docInfo.id).map(annotations =>
        annotations.map(_._1).groupBy(_.annotates.filepartId))
        
    val f = for {
      parts <- fParts
      annotations <- fAnnotationsByPart
    } yield (parts, annotations)

    f.map { case (parts, annotationsByPart) =>
      
      val converted = parts.map { case (part, xml) =>
        partToTEI(part, xml, annotationsByPart.get(part.getId).get) }
      
      // TODO temporary hack
      scala.xml.XML.loadString(converted.head)
    }
  }
  
}
package transform.tei

import java.util.UUID
import models.ContentType
import models.annotation.{ Annotation, AnnotationBody, AnnotationStatus, AnnotatedObject }
import models.generated.tables.records.DocumentFilepartRecord
import org.joda.time.DateTime
import org.joox.JOOX._
import org.w3c.dom.Element
import org.w3c.dom.ranges.DocumentRange
import scala.collection.JavaConversions._

object TEITag {
  
  private def toQuoteBody(lastModifiedAt: DateTime, quote: String) =
    AnnotationBody(
      AnnotationBody.QUOTE,
      None, // lastModifiedBy
      lastModifiedAt,
      Some(quote),
      None, // uri
      None, // note
      None  // status
    )
    
  private def toEntityBody(lastModifiedAt: DateTime, entityType: AnnotationBody.Type, ref: Option[String]) =
    AnnotationBody(
      entityType,
      None, // lastModifiedBy
      lastModifiedAt,
      None, // value
      ref,
      None, // note
      Some(AnnotationStatus(
        AnnotationStatus.VERIFIED,
        None,   // setBy
        lastModifiedAt)))
    
  private def toTagBodies(lastModifiedAt: DateTime, attributes: Seq[(String, Seq[String])]) =
    attributes.map { case (key, values) =>
      AnnotationBody(
        AnnotationBody.TAG,
        None, // lastModifiedBy
        lastModifiedAt,
        Some(s"@${key}:${values.mkString}"),
        None, // uri
        None, // note
        None  // status
      )
    }
  
  private def toAnnotation(lastModifiedAt: DateTime, part: DocumentFilepartRecord, anchor: String, bodies: Seq[AnnotationBody]) =
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
      lastModifiedAt,
      bodies)
  
  private def getAnchor(el: Element, ranges: DocumentRange) = {
    val rangeBefore = ranges.createRange()
    rangeBefore.setStart(el.getParentNode(), 0)
    rangeBefore.setEnd(el, 0)
    
    val offset = rangeBefore.toString.size
    val quote = el.getTextContent
    
    val xpath = $(el).xpath
    val xpathNormalized = xpath.substring(0, xpath.lastIndexOf('/')).toLowerCase
      .replaceAll("\\[1\\]", "") // Selecting first is redundant (and browser clients don't do it)
      
    "from=" + xpathNormalized + "::" + offset + ";to=" + xpathNormalized + "::" + (offset + quote.size)
  }
  
  def convert(part: DocumentFilepartRecord, el: Element, ranges: DocumentRange) = {
    val now = DateTime.now
    
    val quote  = el.getTextContent
      
    val attributesAsNodeMap = el.getAttributes() // .map { foo =>
    val attributes = Seq.range(0, attributesAsNodeMap.getLength)
      .map { idx => 
        val node = attributesAsNodeMap.item(idx) 
        (node.getNodeName, node.getNodeValue) }
      .groupBy(_._1)
      .map { case (key, keysAndVals) => (key, keysAndVals.map(_._2)) }
      .toSeq
      
    // 'ref' is treated separately, 'n' is discarded altogether
    val attributesToConvert = attributes.filter { attr =>
      val key = attr._1.toLowerCase
      key != "n" && key != "ref" }
      
    val ref = el.getAttribute("ref") match {
      case s if s.isEmpty => None
      case s => Some(s)
    }
        
    val quoteBody = toQuoteBody(now, quote)
    val tagBodies = toTagBodies(now, attributesToConvert)
    
    val entityBody = el.getNodeName.toLowerCase match {
      case "placename" => Some(toEntityBody(now, AnnotationBody.PLACE, ref))
      case "persname"  => Some(toEntityBody(now, AnnotationBody.PERSON, ref))
      case "span"      => None
    }
    
    val allBodies = entityBody match {
      case Some(b) => Seq(quoteBody, b) ++ tagBodies
      case None    => quoteBody +: tagBodies
    }
    
    val annotation = toAnnotation(now, part, getAnchor(el, ranges), allBodies)
    
    // Now that it's converted to standoff, remove from XML DOM
    $(el).replaceWith(quote)
    
    annotation
  }
  
}
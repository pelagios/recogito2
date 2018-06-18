package transform.tei

import java.util.UUID
import services.ContentType
import services.annotation.{Annotation, AnnotationBody, AnnotationStatus, AnnotatedObject}
import services.annotation.relation.Relation
import services.generated.tables.records.DocumentFilepartRecord
import org.joda.time.DateTime
import org.joox.JOOX._
import org.w3c.dom.Element
import org.w3c.dom.ranges.DocumentRange
import scala.collection.JavaConversions._

object TEITag extends {
  
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
    
  private def toEntityBody(lastModifiedAt: DateTime, entityType: AnnotationBody.Type, ref: Option[String]) = {
    val status = ref.map(_ => // Set to verified only if there is a reference
      AnnotationStatus(AnnotationStatus.VERIFIED, None, lastModifiedAt))
      
    AnnotationBody(
      entityType,
      None, // lastModifiedBy
      lastModifiedAt,
      None, // value
      ref,
      None, // note
      status)
  }
    
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
      bodies,
      Seq.empty[Relation])
  
  private def getAnchor(el: Element, ranges: DocumentRange) = {
    val rangeBefore = ranges.createRange()
    rangeBefore.setStart(el.getParentNode(), 0)
    rangeBefore.setEnd(el, 0)
    
    val offset = rangeBefore.toString.size
    val quote = el.getTextContent
    
    val xpath = $(el).xpath
    val xpathNormalized = xpath.substring(0, xpath.lastIndexOf('/')).toLowerCase
      .replaceAll("\\[1\\]", "") // Selecting first is redundant (and browser clients don't do it)
      
    s"from=${xpathNormalized}::${offset};to=${xpathNormalized}::${offset + quote.size}"
  }
  
  def convert(part: DocumentFilepartRecord, el: Element, ranges: DocumentRange) = {
    
    def getAttribute(key: String) = el.getAttribute(key) match {
      case s if s.isEmpty => None
      case s => Some(s)
    }
    
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
      key != "xml:id" && key != "ref" }
      
    val ref = getAttribute("ref")  
    val t   = getAttribute("type")
    val quoteBody = toQuoteBody(now, quote)
    val tagBodies = toTagBodies(now, attributesToConvert)
    
    val entityBody = el.getNodeName.toLowerCase match {
      case "placename"                 => Some(toEntityBody(now, AnnotationBody.PLACE, ref))
      case "persname"                  => Some(toEntityBody(now, AnnotationBody.PERSON, ref))
      case "rs" if t == Some("event")  => Some(toEntityBody(now, AnnotationBody.EVENT, None))
      case "span"                      => None
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
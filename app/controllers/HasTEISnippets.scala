package controllers

import java.io.{File, FileReader, StringReader}
import javax.xml.parsers.DocumentBuilderFactory
import org.joox.JOOX._
import org.w3c.dom.{Document, Element, Node}
import org.w3c.dom.ranges.{DocumentRange, Range}
import org.xml.sax.InputSource

trait HasTEISnippets extends HasTextSnippets {
  
  private val DEFAULT_BUFFER_SIZE = 80

  case class TEIAnchor(startPath: String, startOffset: Int, endPath: String, endOffset: Int)
    
  private[controllers] def parseAnchor(anchor: String) = {
    
    def separate(a: String): (String, Int) = {
      val path = a.substring(0, a.indexOf("::")).replaceAll("tei", "TEI")
      val offset = a.substring(a.indexOf("::") + 2).toInt
      (path, offset)
    }
    
    val (startPath, startOffset) = separate(anchor.substring(5, anchor.indexOf(";")))
    val (endPath, endOffset) = separate(anchor.substring(anchor.indexOf(";") + 4))
      
    TEIAnchor(startPath, startOffset, endPath, endOffset)    
  }
  
  /** Given a starting node, this function 'flattens' the DOM, depth first.
    *  
    * E.g. the following structure
    * 
    * <a>
    *   <b>
    *     <c></c>
    *     <d></d>
    *   </b>
    *   <e>
    *     <f></f>
    *     <g>
    *       <h></h>
    *       <i></i>
    *     </g>
    *   </e>
    * </a>  
    * 
    * will result in the list
    * 
    * (a, b, c, d, e, f, g, h, i)
    */
  private[controllers] def flattenDOM(node: Node, flattened: Seq[Node] = Seq.empty[Node]): Seq[Node] = {    
    (Option(node.getFirstChild), Option(node.getNextSibling)) match {
      
      case (Some(child), Some(sibling)) =>
        // Walk child level and then continue with sibling
        val nodes = flattenDOM(child, flattened :+ node)
        flattenDOM(sibling, nodes)      
        
      case (Some(child), None) =>
        // Append and continue with child
        flattenDOM(child, flattened :+ node)
        
      case (None, Some(sibling)) =>
        // Append and continue with sibling
        flattenDOM(sibling, flattened :+ node)
        
      case (None, None) =>
        // Just append this node and we're done
        flattened :+ node
        
    }
  }
  
  private def parseXML(source: InputSource) = {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    builder.parse(source)
  }
  
  protected def parseXMLString(xml: String) = 
    parseXML(new InputSource(new StringReader(xml)))
  
  protected def parseXMLFile(file: File) = 
    parseXML(new InputSource(new FileReader(file)))
  
  protected def toRange(anchor: String, doc: Document): Range = {
    
    def findPosition(parent: Element, offset: Int): (Node, Int) = {
      val firstChild = parent.getFirstChild
      val len = Option(firstChild.getNodeValue).map(_.size).getOrElse(0)
      
      if (offset <= len) {
        // Offset doesn't cross node boundaries
        (firstChild, offset)
      } else {
        // Offset exceeds first child boundaries - step through DOM
        def findRecursive(remainingNodes: Seq[Node], remainingOffset: Int): (Node, Int) = {
          val h = remainingNodes.head
          val t = remainingNodes.tail
          val len = Option(h.getNodeValue).map(_.size).getOrElse(0)
          if (len >= remainingOffset)
            (h, remainingOffset)
          else 
            findRecursive(t, remainingOffset - len)
        }
        
        findRecursive(flattenDOM(firstChild), offset)
      }        
    }
    
    val ranges = doc.asInstanceOf[DocumentRange]
    val a = parseAnchor(anchor)

    val startParent = $(doc).xpath(a.startPath).get(0)
    val (startNode, startOffset) = findPosition(startParent, a.startOffset)
    
    val endParent = $(doc).xpath(a.endPath).get(0)
    val (endNode, endOffset) = findPosition(endParent, a.endOffset)

    val range = ranges.createRange()
    range.setStart(startNode, startOffset)
    range.setEnd(endNode, endOffset)
    range
  }
  
  def snippetFromTEIFile(file: File, anchor: String, bufferSize: Int = DEFAULT_BUFFER_SIZE) =
    extractSnippet(parseXMLFile(file), anchor, bufferSize)
  
  def snippetFromTEI(xml: String, anchor: String, bufferSize: Int = DEFAULT_BUFFER_SIZE) =
    extractSnippet(parseXMLString(xml), anchor, bufferSize)
    
  private def extractSnippet(doc: Document, anchor: String, bufferSize: Int) = {
    val ranges = doc.asInstanceOf[DocumentRange]
       
    val selectedRange = toRange(anchor, doc)
    val selectedQuote = selectedRange.toString
    
    val rangeBefore = ranges.createRange()
    rangeBefore.setStart(doc, 0)
    rangeBefore.setEnd(selectedRange.getStartContainer, selectedRange.getStartOffset)
    val trimmedBefore = { 
      val r = rangeBefore.toString
      
      // Make sure we have more than just the buffer size,
      // otherwise the triming algo will treat it as start of sentence
      r.substring(Math.max(r.size - bufferSize - 1, 0))
    }
    
    val rangeAfter = ranges.createRange()
    rangeAfter.setStart(selectedRange.getEndContainer, selectedRange.getEndOffset)
    rangeAfter.setEnd(doc, 0)
    val trimmedAfter = rangeAfter.toString.substring(0, bufferSize)
    
    val text = trimmedBefore + selectedQuote + trimmedAfter
    snip(text, trimmedBefore.size, selectedQuote.size, bufferSize)
  }
  
  def previewFromTEI(xml: String, len: Int = 256): String = {
    val doc = parseXMLString(xml)
    val body = $(doc).find("body")
    normalize($(body).text).substring(0, len)
  }
  
}
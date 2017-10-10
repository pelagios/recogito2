package controllers

import java.io.{ File, FileReader, StringReader }
import javax.xml.parsers.DocumentBuilderFactory
import org.joox.JOOX._
import org.w3c.dom.Document
import org.w3c.dom.ranges.{ DocumentRange, Range }
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
    val ranges = doc.asInstanceOf[DocumentRange]
    val a = parseAnchor(anchor)

    val startNode = $(doc).xpath(a.startPath).get(0).getFirstChild
    val endNode = $(doc).xpath(a.endPath).get(0).getFirstChild

    // TODO this will break in many cases - we need to re-implement the clientside "reanchor" feature
    val range = ranges.createRange()
    range.setStart(startNode, a.startOffset)
    range.setEnd(endNode, a.endOffset)
    range
  }
  
  def extractTEISnippet(file: File, anchor: String, bufferSize: Int = DEFAULT_BUFFER_SIZE) = {
    val doc = parseXMLFile(file)
    val ranges = doc.asInstanceOf[DocumentRange]
       
    val selectedRange = toRange(anchor, doc)
    val selectedQuote = selectedRange.toString
    
    val rangeBefore = ranges.createRange()
    rangeBefore.setStart(doc, 0)
    rangeBefore.setEnd(selectedRange.getStartContainer, selectedRange.getStartOffset)
    val trimmedBefore = { 
      val r = rangeBefore.toString
      r.substring(r.size - bufferSize)
    }
    
    val rangeAfter = ranges.createRange()
    rangeAfter.setStart(selectedRange.getEndContainer, selectedRange.getEndOffset)
    rangeAfter.setEnd(doc, 0)
    val trimmedAfter = rangeAfter.toString.substring(0, bufferSize)
    
    val text = trimmedBefore + selectedQuote + trimmedAfter
    snip(text, trimmedBefore.size, selectedQuote.size, bufferSize)
  }
  
}
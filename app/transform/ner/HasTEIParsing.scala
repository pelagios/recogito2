package transform.ner

import java.io.{File, FileOutputStream, Writer, PrintWriter}
import org.joox.JOOX._
import org.pelagios.recogito.sdk.ner.{Entity, EntityType}
import org.w3c.dom.Node
import org.w3c.dom.ranges.DocumentRange

trait HasTEIParsing {
  
  /** Recursively returns all text node children of a given root node **/
  private def flattenTextNodes(node: Node, flattened: Seq[Node] = Seq.empty[Node]): Seq[Node] = {
    if (node.getNodeType == Node.TEXT_NODE) {
      if (node.getNodeValue.trim.isEmpty)
        flattened
      else
        flattened :+ node
    } else {
      val children = node.getChildNodes()
      val childrenAsSeq = Seq.tabulate(children.getLength)(n => children.item(n))
      childrenAsSeq.flatMap(child => flattenTextNodes(child, flattened))
    } 
  }

  /** NER-parses TEI **/
  private[ner] def parseTEI(file: File): Seq[(Entity, String)] = {    
    val tei = $(file)
    val textElement = tei.find("text").get(0)
    val textNodes = flattenTextNodes(textElement)

    val ranges = tei.document().asInstanceOf[DocumentRange]
    
    textNodes.foldLeft(Seq.empty[(Entity, String)]) { case (allEntities, textNode) =>
      val parentNode = textNode.getParentNode
      val xpath = $(parentNode).xpath()
      val normalized = xpath.toLowerCase.replaceAll("\\[1\\]", "")
        
      val entities = NERService.parseText(textNode.getNodeValue)
            
      allEntities ++ entities.map { e => 
        val rangeBefore = ranges.createRange()
        rangeBefore.setStart(parentNode, 0)
        rangeBefore.setEnd(textNode, e.charOffset)
        
        val offset = rangeBefore.toString.size
        
        val anchor = s"from=${normalized}::${offset};to=${normalized}::${offset + e.chars.size}"
        (e, anchor)
      }
    } filter { case (e, anchor) =>
      // Bit of a hack, but we need to filter all annotations inside existing place/persNames, since these will
      // be removed in the import stage
      !(anchor.contains("placename") || anchor.contains("persname")) 
    }
  }
  
  /** Parses the TEI, enriching the original XML with the result <placeName> and <persName> tags.  
    *  
    * If an outfile is provided, the results will be written there. Otherwise the original XML file
    * is replaced with the enriched one.
    */
  private[ner] def enrichTEI(file: File, writer: Option[Writer] = None) = {

    def insertMarkup(text: String, entity: Entity, runningOffset: Int, tagName: String) = {
      val len = entity.chars.size
            
      val rangeBefore = text.substring(0, entity.charOffset + runningOffset)
      val rangeAfter = text.substring(entity.charOffset + runningOffset + len)
                    
      s"${rangeBefore}<${tagName}>${entity.chars}</${tagName}>${rangeAfter}"
    }
      
    val doc = $(file)
    val text = doc.find("text")
    val textNodes = flattenTextNodes(text.get(0))
    
    // Replace nodes with enriched copies in-place
    textNodes.foreach { node =>
      val textBefore = node.getNodeValue
      val entities = NERService.parseText(textBefore).sortBy(_.charOffset)
      val enriched = entities.foldLeft(textBefore, 0) { case ((text, runningOffset), entity) =>
        entity.entityType match {
          
          case EntityType.LOCATION =>
            val enriched = insertMarkup(text, entity, runningOffset, "placeName")                    
            (enriched, runningOffset + 23)
            
          case EntityType.PERSON =>
            val enriched = insertMarkup(text, entity, runningOffset, "persName")
            (enriched, runningOffset + 21)

          case _ =>
            (text, runningOffset)
        }
      }

      // Buffer owner doc and parent node ref
      val ownerDoc = node.getOwnerDocument
      val parentNode = node.getParentNode

      // Convert the XML string to a list of XML nodes
      val wrappedSegmentsAsString = $(s"<recogito-replacement>${enriched._1}</recogito-replacement>").get(0)
      val asNodeList = wrappedSegmentsAsString.getChildNodes
      val asNodes = Seq.tabulate(asNodeList.getLength)(n => ownerDoc.importNode(asNodeList.item(n), true))
      
      // Insert each child before the original text node...
      asNodes.foreach(n => parentNode.insertBefore(n, node))
      
      // ...and then delete the original from the DOM
      parentNode.removeChild(node)
    }
    
    val w = writer match {
      case Some(w) => w        
      case None => new PrintWriter(new FileOutputStream(file, false))
    }
    
    w.write(doc.toString)
    w.close()
  }

  
}
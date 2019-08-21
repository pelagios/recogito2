package transform.ner

import java.io.{File, FileOutputStream, Writer, PrintWriter}
import org.joox.JOOX._
import org.pelagios.recogito.sdk.ner.{Entity, EntityType}
import org.w3c.dom.{Node, NodeList}
import org.w3c.dom.ranges.DocumentRange
import play.api.Configuration

trait HasTeiNER {
  
  private def toList(nodeList: NodeList) =
    Seq.tabulate(nodeList.getLength)(n => nodeList.item(n))
  
  /** Recursively returns all text node children of a given root node as a list **/
  private def flattenTextNodes(node: Node, flattened: Seq[Node] = Seq.empty[Node]): Seq[Node] = {
    if (node.getNodeType == Node.TEXT_NODE) {
      if (node.getNodeValue.trim.isEmpty) flattened // Don't add empty text nodes
      else flattened :+ node
    } else {
      val children = toList(node.getChildNodes)
      children.flatMap(child => flattenTextNodes(child, flattened))
    } 
  }

  /** Runs NER on a TEI document.
    *
    * Method: to preserve TEI DOM without going overboard with complexity, each text node
    * in the DOM is feed into NER separately. Since the resulting (NER SDK) Entity objects
    * include only the offset, inside that text node, the method also computes the global
    * XPath position separately and returns it along with the entity. 
    */
  private[ner] def parseTEI(file: File, engine: Option[String], config: Configuration): Seq[(Entity, String)] = {    
    val tei = $(file)
    val ranges = tei.document().asInstanceOf[DocumentRange]
    
    val textElement = tei.find("text").get(0) // TEI <text> element
    val textNodes = flattenTextNodes(textElement) // Text nodes contained inside <text></text>
    
    textNodes.foldLeft(Seq.empty[(Entity, String)]) { case (results, textNode) =>
      val parentNode = textNode.getParentNode
      val xpath = $(parentNode).xpath()
        
      val entities = NERService.parseText(textNode.getNodeValue, engine, config)
            
      results ++ entities.map { entity => 
        val rangeBefore = ranges.createRange()
        rangeBefore.setStart(parentNode, 0)
        rangeBefore.setEnd(textNode, entity.charOffset)
        
        val offset = rangeBefore.toString.size
        val anchor = s"from=${xpath}::${offset};to=${xpath}::${offset + entity.chars.size}"
        
        (entity, anchor)
      }
    } filter { case (_, anchor) => // Don't include annotations inside existing place/persNames
      !(anchor.contains("placename") || anchor.contains("persname")) 
    }
  }
  
  /** Alternative parsing approach: inlines results instead of generating standoff annotations.
    *  
    * If a writer is provided, the results will be written there. Otherwise the original XML file
    * is replaced with the enriched document.
    * 
    * This implementation isn't used in Recogito at the moment. But we it might be handy later at
    * some point.
    */
  private[ner] def enrichTEI(file: File, engine: Option[String], config: Configuration, writer: Option[Writer] = None) = {

    def replaceTextNode(textNode: Node, enrichedText: String) = {
      // Buffer owner doc and parent node reference
      val ownerDoc = textNode.getOwnerDocument
      val parentNode = textNode.getParentNode

      // Convert the serialized XML string to a list of XML nodes by wrapping it and parsing with JOOX
      val wrapped = $(s"<wrapper>${enrichedText}</wrapper>").get(0)
      val nodes = toList(wrapped.getChildNodes)
      
      // Insert each child before the original text node...
      nodes.foreach { node =>
        val imported = ownerDoc.importNode(node, true) 
        parentNode.insertBefore(imported, node)
      }
      
      // ...and delete the original node from the DOM
      parentNode.removeChild(textNode)
    }
    
    // Helper to insert entity markup into text 
    def insertMarkup(text: String, entity: Entity, runningOffset: Int, tagName: String) = {
      val len = entity.chars.size
            
      val textBefore = text.substring(0, entity.charOffset + runningOffset)
      val textAfter = text.substring(entity.charOffset + runningOffset + len)
                    
      s"${textBefore}<${tagName}>${entity.chars}</${tagName}>${textAfter}"
    }
      
    val tei = $(file)
    val textElement = tei.find("text").get(0)
    val textNodes = flattenTextNodes(textElement)
    
    textNodes.foreach { node =>
      val originalText = node.getNodeValue
      
      val entities = NERService.parseText(originalText, engine, config).sortBy(_.charOffset)
      
      // Original text + inserted <placeName> and <persName> tags
      val enrichedText = entities.foldLeft(originalText, 0) { case ((text, runningOffset), entity) =>
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

      replaceTextNode(node, enrichedText._1)
    }
    
    val w = writer match {
      case Some(w) => w        
      case None => new PrintWriter(new FileOutputStream(file, false))
    }
    
    w.write(tei.toString)
    w.close()
  }
  
}
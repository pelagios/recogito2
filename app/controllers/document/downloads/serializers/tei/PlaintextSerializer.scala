package controllers.document.downloads.serializers.tei

import services.annotation.{ Annotation, AnnotationBody, AnnotationService }
import services.document.{ DocumentInfo, DocumentService }
import play.api.mvc.{ AnyContent, Request }
import scala.concurrent.{ Future, ExecutionContext }
import scala.xml.{ UnprefixedAttribute, Node, Null, Text }
import storage.uploads.Uploads

trait PlaintextSerializer extends BaseTEISerializer {
  
  /** Simplistic, but should be all we need. If we need more, we can switch to Apache Commons StringEscapeUtils **/
  private def escape(str: String) =
    str.replace("<", "&lt;")
       .replace(">", "&gt;")
  
  private def textpartToTEI(text: String, annotations: Seq[Annotation]): Seq[Node] = {
    
    import AnnotationBody._
    
    // Shorthands for convenience
    def getQuote(annotation: Annotation) =
      annotation.bodies.find(_.hasType == AnnotationBody.QUOTE).head.value.get
      
    def getCharOffset(annotation: Annotation) =
      annotation.anchor.substring(annotation.anchor.indexOf(":") + 1).toInt
      
    def getEntityType(annotation: Annotation) = 
      annotation.bodies.find(b => Set(PLACE, PERSON, EVENT).contains(b.hasType))
    
    // XML, by nature can't handle overlapping annotations
    val nonOverlappingAnnotations = sort(annotations).foldLeft(Seq.empty[Annotation]) { case (result, next) =>
      result.lastOption match {
        case Some(previous) =>
          // Check if next overlaps previous
          val previousQuote = getQuote(previous)
          val previousOffset = getCharOffset(previous)
          
          val nextOffset = getCharOffset(next)
          
          if (nextOffset >= previousOffset + previousQuote.size)
            result :+ next
          else
            result
          
        case None =>
          // First in line
          Seq(next)
      }
    }
    
    val ranges = nonOverlappingAnnotations.foldLeft((Seq.empty[Node], 0)) { case ((nodes, beginIndex), annotation) =>
      val id = annotation.annotationId.toString
      val quote = escape(getQuote(annotation))
      val offset = getCharOffset(annotation)
      val entityType = getEntityType(annotation)      
      val attributes = getAttributes(annotation)
      
      val baseTag = entityType.map { body =>
        body.hasType match {        
          case PLACE => body.uri match {
            case Some(uri) => <placeName ref={uri} n={id}>{quote}</placeName>
            case None => <placeName n={id}>{quote}</placeName>
          }
            
          case PERSON =>
            <persName n={id}>{quote}</persName>
            
          case EVENT =>
            <rs n={id} type="event">{quote}</rs>
        }
      }.getOrElse(<span>{quote}</span>)
      
      val teiTag = attributes.foldLeft(baseTag) { case (el, (name, values)) =>
        el % new UnprefixedAttribute(name, Text(values.mkString), Null)
      }
      
      val nextNodes = 
        Seq(new Text(escape(text.substring(beginIndex, offset))), teiTag)
          
      (nodes ++ nextNodes, offset + quote.size)
    }
    
    val remainder = escape(text.substring(ranges._2))
    ranges._1 :+ new Text(remainder)
  }
  
  def plaintextToTEI(doc: DocumentInfo)(implicit documentService: DocumentService,
      uploads: Uploads, annotationService: AnnotationService, request: Request[AnyContent], ctx: ExecutionContext) = {
    
    val fTexts = Future.sequence {
      doc.fileparts.map { part =>
        uploads.readTextfile(doc.owner.getUsername, doc.id, part.getFile).map(_.map((_, part)))
      }
    }
    
    val fDivs = fTexts.flatMap { maybeTextsAndParts => 
      val textsAndParts = maybeTextsAndParts.flatten
      val fAnnotations = annotationService.findByDocId(doc.id)
      
      fAnnotations.map { t =>
        val annotationsByPart = t.map(_._1).groupBy(_.annotates.filepartId)
        textsAndParts.map { case (text, part) =>
          <div><p>{ textpartToTEI(text, annotationsByPart.get(part.getId).getOrElse(Seq.empty[Annotation])) }</p></div>
        }
      }
    }
    
    fDivs.map { divs =>
      <TEI xmlns="http://www.tei-c.org/ns/1.0">
        <teiHeader>
          <fileDesc>
            <titleStmt><title>{ doc.author.map(_ + ": ").getOrElse("") }{ doc.title }</title></titleStmt>
            <publicationStmt>
              { 
                (doc.dateFreeform, doc.dateNumeric) match {
                  case (Some(df), Some(dn)) =>
                    <p><date when={ dn.toString }>{ df }</date></p>
                    
                  case (Some(df), None) =>
                    <p>{ df }</p>
                    
                  case (None, Some(dn)) =>
                    <p><date when={ dn.toString}>{ dn.toString }</date></p>
                    
                  case _ => <p/>
                }
              }              
            </publicationStmt>
            <sourceDesc>
              <p><link target={ controllers.document.routes.DocumentController.initialDocumentView(doc.id).absoluteURL } /></p> 
            </sourceDesc>
          </fileDesc>
          <encodingDesc>
            <projectDesc><p>Downloaded from { controllers.landing.routes.LandingController.index().absoluteURL}</p></projectDesc>
          </encodingDesc>
        </teiHeader>
        <text>
          <body>{ divs }</body>
        </text>
      </TEI>
    }
  }
  
}
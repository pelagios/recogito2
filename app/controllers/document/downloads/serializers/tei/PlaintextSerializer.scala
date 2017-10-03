package controllers.document.downloads.serializers.tei

import controllers.document.downloads.serializers.BaseSerializer
import models.annotation.{ Annotation, AnnotationBody, AnnotationService }
import models.document.DocumentService
import scala.concurrent.{ Future, ExecutionContext }
import scala.xml.{ Node, Text }
import storage.Uploads
import models.document.DocumentInfo

trait PlaintextSerializer extends BaseSerializer {
  
  /** Simplistic, but should be all we need. If we need more, we can switch to Apache Commons StringEscapeUtils **/
  private def escape(str: String) =
    str.replace("<", "&lt;")
       .replace(">", "&gt;")
  
  private def textpartToTEI(text: String, annotations: Seq[Annotation]): Seq[Node] = {    
    
    // Shorthands for convenience
    def getQuote(annotation: Annotation) =
      annotation.bodies.find(_.hasType == AnnotationBody.QUOTE).head.value.get
      
    def getCharOffset(annotation: Annotation) =
      annotation.anchor.substring(annotation.anchor.indexOf(":") + 1).toInt
      
    // For the time being, TEI will only include place annotations
    val placeAnnotations = sort(annotations.filter(_.bodies.exists(_.hasType == AnnotationBody.PLACE)))
    
    // XML, by nature can't handle overlapping annotations
    val nonOverlappingPlaceAnnotations = placeAnnotations.foldLeft(Seq.empty[Annotation]) { case (result, next) =>
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
    
    val ranges = nonOverlappingPlaceAnnotations.foldLeft((Seq.empty[Node], 0)) { case ((nodes, beginIndex), annotation) =>
      val quote = getQuote(annotation)
      val offset = getCharOffset(annotation)
      val maybeURI = annotation.bodies.find(_.hasType == AnnotationBody.PLACE).head.uri
      
      val placeTag = maybeURI match {
        case Some(uri) =>
          <placeName ref={ uri }>{ escape(quote) }</placeName>
        case None =>
          <placeName>{ escape(quote) }</placeName>
      }
      
      val nextNodes = 
        Seq(new Text(escape(text.substring(beginIndex, offset))), placeTag)
          
      (nodes ++ nextNodes, offset + quote.size)
    }
    
    val remainder = escape(text.substring(ranges._2))
    ranges._1 :+ new Text(remainder)
  }
  
  def documentToTEI(docInfo: DocumentInfo)(implicit documentService: DocumentService,
      uploads: Uploads, annotationService: AnnotationService, ctx: ExecutionContext) = {
    
    val fTexts = Future.sequence {
      docInfo.fileparts.map { part =>
        uploads.readTextfile(docInfo.owner.getUsername, docInfo.id, part.getFile).map(_.map((_, part)))
      }
    }
    
    val fDivs = fTexts.flatMap { maybeTextsAndParts => 
      val textsAndParts = maybeTextsAndParts.flatten
      val fAnnotations = annotationService.findByDocId(docInfo.id)
      
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
            <titleStmt><title>{ docInfo.author.map(_ + ": ").getOrElse("") }{ docInfo.title }</title></titleStmt>
            <publicationStmt>
              { 
                (docInfo.dateFreeform, docInfo.dateNumeric) match {
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
              { 
                docInfo.source match { 
                  case Some(s) => <p><link target={s} /></p>
                  case _ => <p/> 
                }
              }
            </sourceDesc>
          </fileDesc>
        </teiHeader>
        <text>
          <body>{ divs }</body>
        </text>
      </TEI>
    }
  }
  
}
package controllers.document.downloads.serializers.document.tei

import java.sql.Timestamp
import java.text.SimpleDateFormat
import services.annotation.{Annotation, AnnotationBody, AnnotationService}
import services.entity.EntityType
import services.entity.builtin.EntityService
import services.document.{ExtendedDocumentMetadata, DocumentService}
import play.api.mvc.{AnyContent, Request}
import scala.concurrent.{Future, ExecutionContext}
import scala.xml.{Elem, UnprefixedAttribute, Node, Null, Text}
import storage.es.ES
import storage.uploads.Uploads

trait PlaintextToTEI extends BaseTEISerializer {

  import AnnotationBody._

  /** Simplistic, but should be all we need. If we need more, we can switch to Apache Commons StringEscapeUtils **/
  private def escape(str: String) =
    str.replace("<", "&lt;")
       .replace(">", "&gt;")

  private def formatDate(t: Timestamp): String =
    new SimpleDateFormat("yyyy-MM-dd").format(t)
  
  /** Shorthands for convenience **/
  private def getQuote(annotation: Annotation) =
    annotation.bodies.find(_.hasType == AnnotationBody.QUOTE).head.value.get
    
  private def getCharOffset(annotation: Annotation) =
    annotation.anchor.substring(annotation.anchor.indexOf(":") + 1).toInt
    
  private def getEntityType(annotation: Annotation) = 
    annotation.bodies.find(b => Set(PLACE, PERSON, EVENT).contains(b.hasType))

  private def paragraphToTEI(text: String, annotations: Seq[Annotation], runningOffset: Int = 0): Seq[Node] = {

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
      val id = toTeiId(annotation.annotationId)
      val quote = escape(getQuote(annotation))
      val offset = getCharOffset(annotation) - runningOffset
      val entityType = getEntityType(annotation)  

      // Commentary notes
      val notes = getCommentBodies(annotation).flatMap { comment => 
        <note target={id} resp={comment.lastModifiedBy.get}>{comment.value.get}</note>
      }
      
      // Tags of form @key:value - to be used as XML attributes
      val attributes = getAttributeTags(annotation)
      
      // All other tags, rolled into one 'ana' attribute
      val tags = getNonAttributeTags(annotation)
      val ana = { if (tags.isEmpty) None else Some(tags.mkString(",")) }.map { xml.Text(_) }
      
      // Cert (if any), derived from annotation status
      val cert = getCert(annotation).map(xml.Text(_)) 
      
      val baseTag = entityType.map { body =>
        body.hasType match {        
          case PLACE => body.uri match {
            case Some(uri) => 
              <placeName ref={uri} xml:id={id} ana={ana} cert={cert}>{quote}</placeName>
            
            case None => 
              <placeName xml:id={id} ana={ana} cert={cert}>{quote}</placeName>
          }
            
          case PERSON =>
            <persName xml:id={id} ana={ana}>{quote}</persName>
            
          case EVENT =>
            <rs xml:id={id} type="event" ana={ana}>{quote}</rs>
        }
      }.getOrElse(<span xml:id={id} ana={ana}>{quote}</span>)
      
      val teiTag = attributes.foldLeft(baseTag) { case (el, (name, values)) =>
        el % new UnprefixedAttribute(name, Text(values.mkString), Null)
      }
      
      val nextNodes = 
        Seq(new Text(escape(text.substring(beginIndex, offset))), teiTag) ++ notes
          
      (nodes ++ nextNodes, offset + quote.size)
    }
    
    val remainder = escape(text.substring(ranges._2))
    ranges._1 :+ new Text(remainder)
  }
  
  def plaintextToTEI(
    doc: ExtendedDocumentMetadata
  )(implicit documentService: DocumentService,
      annotationService: AnnotationService, 
      entityService: EntityService,
      uploads: Uploads, 
      request: Request[AnyContent], 
      ctx: ExecutionContext
  ) = {
    
    val fTexts = Future.sequence {
      doc.fileparts.map { part =>
        uploads.readTextfile(doc.owner.getUsername, doc.id, part.getFile).map(_.map((_, part)))
      }
    }
    
    val fDivs = fTexts.flatMap { maybeTextsAndParts => 
      val textsAndParts = maybeTextsAndParts.flatten
      val fAnnotations = annotationService.findByDocId(doc.id)
      val fPlaces = entityService.listEntitiesInDocument(doc.id, Some(EntityType.PLACE), 0, ES.MAX_SIZE)

      val f = for {
        annotations <- fAnnotations
        places <- fPlaces
      } yield (
        annotations.map(_._1),
        places.items.map(_._1).map(_.entity)
      )

      f.map { case (annotations, places) =>        
        val annotationsByPart = annotations.groupBy(_.annotates.filepartId)

        val divs = textsAndParts.map { case (text, part) =>
          // Split text to paragraph
          val paragraphs = text.split("\n\n")

          val pTags = paragraphs.foldLeft((Seq.empty[Elem], 0)) { case ((elements, runningOffset), text) => {
            val textLength = text.size

            // Filter annotations for this paragraph
            val annotationsInParagraph = 
              annotationsByPart
                .get(part.getId).getOrElse(Seq.empty[Annotation])
                .filter(annotation => {
                  val offset = getCharOffset(annotation)
                  val quoteLength = getQuote(annotation).size

                  // Keep only annotations contained entirely within this paragraph
                  offset >= runningOffset && (offset + quoteLength) <= (runningOffset + textLength)
                })

            val p = <p>{ paragraphToTEI(text, annotationsInParagraph, runningOffset) }</p>
            (elements :+ p, runningOffset + text.size + 2)
          }}._1

          <div>{pTags}</div>
        }
        
        val listPlaces = placesToList(annotations, places)
        val relations = relationsToList(annotations)
        (divs, listPlaces, relations)
      }
    }
       
    fDivs.map { case (divs, listPlaces, relations) =>
      <TEI xmlns="http://www.tei-c.org/ns/1.0">
        <teiHeader>
          <fileDesc>
            <titleStmt>
              <title>{ doc.author.map(_ + ": ").getOrElse("") }{ doc.title }</title>
              <author>{ doc.ownerName }</author>
            </titleStmt>
            <publicationStmt>
              <p><date>{formatDate(doc.uploadedAt)}</date></p>             
            </publicationStmt>
            <sourceDesc>
              <bibl source={controllers.document.routes.DocumentController.initialDocumentView(doc.id).absoluteURL } />
              <biblStruct>
                <monogr>
                  { if (doc.author.isDefined) 
                    <author>{doc.author.get}</author>
                  }
                  <title>{ doc.title }</title>
                  { (doc.dateFreeform, doc.dateNumeric) match {	
                    
                    case (Some(df), Some(dn)) =>	
                      <imprint><date when={ dn.toString }>{ df }</date></imprint>

                    case (Some(df), None) =>	
                      <imprint><date>{ df }</date></imprint>	

                    case (None, Some(dn)) =>	
                      <imprint><date when={ dn.toString}>{ dn.toString }</date></imprint>

                    case _ => 	
                  }}  
                </monogr>
              </biblStruct>
            </sourceDesc>
          </fileDesc>
          { if (relations.isDefined || doc.language.isDefined || listPlaces.isDefined)
            <profileDesc>
              { if (doc.language.isDefined)
                <langUsage>
                  <language ident={doc.language.get} />
                </langUsage>
              }
              { if (relations.isDefined || listPlaces.isDefined)
                <particDesc>
                  { if (listPlaces.isDefined) listPlaces.get }
                  { if (relations.isDefined) relations.get }
                </particDesc>
              }
            </profileDesc>
          }
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
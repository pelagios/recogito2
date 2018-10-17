package controllers.document.downloads.serializers.document.tei

import controllers.HasTEISnippets
import services.annotation.{ Annotation, AnnotationBody, AnnotationService }
import services.document.{ DocumentInfo, DocumentService }
import services.generated.tables.records.DocumentFilepartRecord
import org.joox.Match
import org.joox.JOOX._
import org.w3c.dom.Document
import scala.concurrent.{ ExecutionContext, Future }
import scala.xml.Elem
import storage.uploads.Uploads

trait TEIToTEI extends BaseTEISerializer with HasTEISnippets {

  private val ENTITY_TYPES = Set(AnnotationBody.PLACE, AnnotationBody.PERSON, AnnotationBody.EVENT)

  /** Not really a sort, at least not within the DOM. But sort only matter only within
    * each tag individually. Therefore, it's sufficient if we naively sort everything
    * by start offset.
    */
  private def sortByOffsetDesc(annotations: Seq[Annotation]): Seq[Annotation] = annotations.sortBy { annotation =>
    val a = annotation.anchor
    val startOffset = a.substring(a.indexOf("::") + 2, a.indexOf(";")).toInt
    -startOffset
  }
  
  /** Returns the sourceDesc element of the TEI document, creating it in place if it doesn't exist already **/
  private[tei] def getOrCreateSourceDesc(document: Document) = {
    
    def getOrCreate(parent: Match, childName: String, prepend: Boolean): Match = {
      val maybeChild = parent.find(childName)
      if (maybeChild.isEmpty) {
        if (prepend) parent.prepend($(s"<${childName}/>"))
        else parent.append($(s"<${childName}/>"))
        parent.find(childName)
      } else {
        maybeChild
      }
    }
        
    val doc = $(document)
    val teiHeader = getOrCreate(doc, "teiHeader", true) // prepend
    val fileDesc = getOrCreate(teiHeader, "fileDesc", false) // append
    getOrCreate(fileDesc, "sourceDesc", false) // append
  }

  def partToTEI(part: DocumentFilepartRecord, xml: String, annotations: Seq[Annotation]) = {
    val doc = parseXMLString(xml)
   
    def toTag(annotation: Annotation) = {
      val quote = annotation.bodies.find(_.hasType == AnnotationBody.QUOTE).get.value.get
      val entityBody = annotation.bodies.find(b => ENTITY_TYPES.contains(b.hasType))
      val entityURI = entityBody.flatMap(_.uri)
      
      val el = entityBody match {
        case Some(b) if b.hasType == AnnotationBody.PLACE => doc.createElement("placeName")
        
        case Some(b) if b.hasType == AnnotationBody.PERSON => doc.createElement("persName")
        
        case Some(b) if b.hasType == AnnotationBody.EVENT => 
          val rs = doc.createElement("rs")
          rs.setAttribute("type", "event")
          rs
          
        case _ => doc.createElement("span")
      }
      
      el.setAttribute("xml:id", toTeiId(annotation.annotationId))
      if (entityURI.isDefined) el.setAttribute("ref", entityURI.get)
      
      getAttributeTags(annotation).foreach { case(key, values) =>
        el.setAttribute(key, values.mkString) 
      }
      
      val tags = getNonAttributeTags(annotation)
      if (tags.size > 0)
        el.setAttribute("ana", tags.mkString(","))
      
      el.appendChild(doc.createTextNode(quote))

      val verificationStatus = annotation.bodies.flatMap(_.status).headOption.map(_.value)
      if (verificationStatus.isDefined)
        el.setAttribute("cert", verificationStatus.get.toString)

      el
    }

    sortByOffsetDesc(annotations).foreach { annotation =>
      val range = toRange(annotation.anchor, doc)

      // We only support TEI export for annotations that don't cross node boundaries
      if (range.getStartContainer == range.getEndContainer) {
        val tag = toTag(annotation)
        range.deleteContents()
        range.surroundContents(tag)
      }
    }
    
    val relations = relationsToList(annotations)
    if (relations.isDefined)
      getOrCreateSourceDesc(doc).append($(relations.get.toString))

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
        partToTEI(part, xml, annotationsByPart.get(part.getId).getOrElse(Seq.empty[Annotation])) 
      }

      scala.xml.XML.loadString(converted.head)
    }
  }

}

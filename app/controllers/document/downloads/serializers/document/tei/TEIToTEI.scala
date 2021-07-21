package controllers.document.downloads.serializers.document.tei

import controllers.HasTEISnippets
import org.joox.Match
import org.joox.JOOX._
import org.w3c.dom.Document
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem
import services.annotation.{Annotation, AnnotationBody, AnnotationService}
import services.entity.{Entity, EntityType}
import services.entity.builtin.EntityService
import services.document.{ExtendedDocumentMetadata, DocumentService}
import services.generated.tables.records.DocumentFilepartRecord
import storage.es.ES
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

  private def getOrCreate(parent: Match, childName: String, prepend: Boolean): Match = {
    val maybeChild = parent.find(childName)
    if (maybeChild.isEmpty) {
      if (prepend) parent.prepend($(s"<${childName}/>"))
      else parent.append($(s"<${childName}/>"))
      parent.find(childName)
    } else {
      maybeChild
    }
  }
  
  /** Returns the sourceDesc element of the TEI document, creating it in place if it doesn't exist already **/
  private[tei] def getOrCreateParticDesc(document: Document) = {        
    val doc = $(document)
    val teiHeader = getOrCreate(doc, "teiHeader", true) // prepend
    val profileDesc = getOrCreate(teiHeader, "profileDesc", false) // append
    getOrCreate(profileDesc, "particDesc", false) // append
  }

  private[tei] def getOrCreateSettingDesc(document: Document) = {
    val doc = $(document)
    val teiHeader = getOrCreate(doc, "teiHeader", true)
    val profileDesc = getOrCreate(teiHeader, "profileDesc", false)
    getOrCreate(profileDesc, "settingDesc", false)
  }

  private[tei] def getOrCreateClassDecl(document: Document) = {
    val doc = $(document)
    val teiHeader = getOrCreate(doc, "teiHeader", true)
    val encodingDesc = getOrCreate(teiHeader, "encodingDesc", false)
    getOrCreate(encodingDesc, "classDecl", false)
  }

  def partToTEI(part: DocumentFilepartRecord, xml: String, annotations: Seq[Annotation], places: Seq[Entity]) = {
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
        el.setAttribute("ana", tags.map(t => s"#$t").mkString(" "))

      el.appendChild(doc.createTextNode(quote))

      val cert = getCert(annotation)
      if (cert.isDefined)
        el.setAttribute("cert", cert.get)

      el
    }

    def getNotes(annotation: Annotation) = {
      getCommentBodies(annotation).map { comment => 
        val noteEl = doc.createElement("note")
        noteEl.setAttribute("target", toTeiId(annotation.annotationId))
        noteEl.setAttribute("resp", comment.lastModifiedBy.get)
        noteEl.appendChild(doc.createTextNode(comment.value.get))
        noteEl
      }
    }

    sortByOffsetDesc(annotations).foreach { annotation =>
      val anchor = parseAnchor(annotation.anchor)

      // We only support TEI export for annotations that don't cross node boundaries
      if (anchor.startPath == anchor.endPath) {
        val range = toRange(annotation.anchor, doc)
        val tag = toTag(annotation)
        
        range.deleteContents()
        range.surroundContents(tag)

        val notes = getNotes(annotation)
        if (notes.size > 0) {
          range.collapse(false)        
          notes.reverse.foreach(n => range.insertNode(n))
        }
      }
    }

    val listPlace = placesToList(annotations, places)
    if (listPlace.isDefined)
      getOrCreateSettingDesc(doc).append($(listPlace.get.toString))

    val taxonomy = tagsToTaxonomy(annotations)
    if (taxonomy.isDefined)
      getOrCreateClassDecl(doc).append($(taxonomy.get.toString))
    
    val relations = relationsToList(annotations)
    if (relations.isDefined)
      getOrCreateParticDesc(doc).append($(relations.get.toString))

    $(doc).toString
  }

  def teiToTEI(
    doc: ExtendedDocumentMetadata
  )(implicit 
      annotationService: AnnotationService,
      entityService: EntityService,
      documentService: DocumentService,
      uploads: Uploads,  ctx: ExecutionContext
  ): Future[Seq[Elem]] = {

    val fParts = Future.sequence(
      doc.fileparts.map { part =>
        uploads.readTextfile(doc.owner.getUsername, doc.id, part.getFile).map { maybeText =>
          // If maybetext is None, integrity is broken -> let's fail
          (part, maybeText.get)
        }
      })

    val fAnnotationsByPart =
      annotationService.findByDocId(doc.id).map(annotations =>
        annotations.map(_._1).groupBy(_.annotates.filepartId))

    val fPlaces = 
      entityService.listEntitiesInDocument(doc.id, Some(EntityType.PLACE), 0, ES.MAX_SIZE)

    val f = for {
      parts <- fParts
      annotations <- fAnnotationsByPart
      places <- fPlaces
    } yield (
      parts, 
      annotations, 
      places.items.map(_._1).map(_.entity)
    )

    f.map { case (parts, annotationsByPart, places) =>
      parts.map { case (part, xml) =>
        val converted = partToTEI(
          part, xml, 
          annotationsByPart.get(part.getId).getOrElse(Seq.empty[Annotation]), 
          places) 

        scala.xml.XML.loadString(converted)
      }
    }
  }

}

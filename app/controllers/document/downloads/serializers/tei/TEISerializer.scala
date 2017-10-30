package controllers.document.downloads.serializers.tei

import controllers.HasTEISnippets
import controllers.document.downloads.serializers.BaseSerializer
import models.annotation.{ Annotation, AnnotationBody, AnnotationService }
import models.document.{ DocumentInfo, DocumentService }
import models.generated.tables.records.DocumentFilepartRecord
import org.joox.JOOX._
import scala.concurrent.{ ExecutionContext, Future }
import scala.xml.Elem
import storage.Uploads

trait TEISerializer extends BaseSerializer with HasTEISnippets {

  private val ENTITY_TYPES = Set(AnnotationBody.PLACE, AnnotationBody.PERSON)

  /** Not really a sort, at least not within the DOM. But sort only matter only within
    * each tag individually. Therefore, it's sufficient if we naively sort everything
    * by start offset.
    */
  private def sortByOffsetDesc(annotations: Seq[Annotation]): Seq[Annotation] = annotations.sortBy { annotation =>
    val a = annotation.anchor
    val startOffset = a.substring(a.indexOf("::") + 2, a.indexOf(";")).toInt
    -startOffset
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
        case _ => doc.createElement("span")
      }

      if (entityURI.isDefined) el.setAttribute("ref", entityURI.get)
      el.appendChild(doc.createTextNode(quote))

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
        partToTEI(part, xml, annotationsByPart.get(part.getId).get) }

      scala.xml.XML.loadString(converted.head)
    }
  }

}

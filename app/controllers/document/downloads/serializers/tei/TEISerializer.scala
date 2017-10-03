package controllers.document.downloads.serializers.tei

import controllers.document.downloads.serializers.BaseSerializer
import models.annotation.{ Annotation, AnnotationService }
import models.document.{ DocumentInfo, DocumentService }
import models.generated.tables.records.DocumentFilepartRecord
import scala.concurrent.{ ExecutionContext, Future }
import scala.xml.Elem
import storage.Uploads

trait TEISerializer extends BaseSerializer {
  
  def partToTEI(part: DocumentFilepartRecord, xml: String, annotations: Seq[Annotation]) = {
    
    
    
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
      
      null
    }
  }
  
}
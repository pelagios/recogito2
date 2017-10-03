package controllers.document.downloads.serializers.tei

import controllers.document.downloads.serializers.BaseSerializer
import models.annotation.AnnotationService
import models.document.{ DocumentInfo, DocumentService }
import scala.concurrent.{ ExecutionContext, Future }
import scala.xml.Elem
import storage.Uploads

trait TEISerializer extends BaseSerializer {
  
  def partToTEI() = ???
  
  
  def teiToTEI(docInfo: DocumentInfo)(implicit documentService: DocumentService,
      uploads: Uploads, annotationService: AnnotationService, ctx: ExecutionContext): Future[Elem] = {
    
    val fParts = Future.sequence(
      docInfo.fileparts.map { part =>
        uploads.readTextfile(docInfo.owner.getUsername, docInfo.id, part.getFile).map { maybeText =>
          // If maybetext is None, integrity is broken -> let's fail
          (part, maybeText.get)
        }
      })

    // TODO
    null
  }
  
}
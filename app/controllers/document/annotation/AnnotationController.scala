package controllers.document.annotation

import controllers.{ BaseOptAuthController, WebJarAssets }
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import models.ContentType
import models.annotation.AnnotationService
import models.document.{ DocumentAccessLevel, DocumentInfo, DocumentService }
import models.generated.tables.records.{ DocumentFilepartRecord, DocumentRecord, UserRecord }
import models.user.UserService
import play.api.{ Configuration, Logger }
import play.api.mvc.{ RequestHeader, Result }
import scala.concurrent.{ ExecutionContext, Future }
import storage.Uploads
import controllers.HasVisitLogging
import models.visit.VisitService
import controllers.HasTEISnippets

@Singleton
class AnnotationController @Inject() (
    val config: Configuration,
    val annotations: AnnotationService,
    val documents: DocumentService,
    val users: UserService,
    val uploads: Uploads,
    implicit val visits: VisitService,
    implicit val webjars: WebJarAssets,
    implicit val ctx: ExecutionContext
  ) extends BaseOptAuthController(config, documents, users)
    with HasTEISnippets
    with HasVisitLogging {

  /** For convenience: redirects to proper annotation view, given various ID combinations  **/
  def resolveAnnotationView(documentId: String, maybePartId: Option[java.util.UUID], maybeAnnotationId: Option[java.util.UUID]) = AsyncStack { implicit request =>

    // Shorthand re-used below
    def partResponse(partId: UUID, okResponse: DocumentFilepartRecord => Result) =
      documents.findPartById(partId).map {
        case Some(part) => okResponse(part)
        case None =>  NotFoundPage
      }

    (maybePartId, maybeAnnotationId) match {

      case (Some(partId), Some(annotationId)) => partResponse(partId, { part =>
        // Redirect to part, with annotation ID appended as fragment
        Redirect(routes.AnnotationController.showAnnotationView(part.getDocumentId, part.getSequenceNo)
          .withFragment(annotationId.toString).toString) })

      case (Some(partId), None) => partResponse(partId, { part =>
        // Redirect to part
        Redirect(routes.AnnotationController.showAnnotationView(part.getDocumentId, part.getSequenceNo)) })

      case (None, Some(annotationId)) =>
        // No part ID? Could fetch from annotation - not used now, but may be implemented later
        Future.successful(InternalServerError)

      case (None, None) =>
        // No part specified - redirect to first part in sequence
        Future.successful(Redirect(routes.AnnotationController.showAnnotationView(documentId, 1)))
    }
  }

  /** Shows the annotation view for a specific document part **/
  def showAnnotationView(documentId: String, seqNo: Int) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user)
    documentPartResponse(documentId, seqNo, maybeUser, { case (doc, currentPart, accesslevel) =>
      if (accesslevel.canRead)
        renderResponse(doc, currentPart, maybeUser, accesslevel)
      else if (loggedIn.isEmpty) // No read rights - but user is not logged in yet
        authenticationFailed(request)
      else
        Future.successful(ForbiddenPage)
    })
  }

  private def renderResponse(
      doc: DocumentInfo,
      currentPart: DocumentFilepartRecord,
      loggedInUser: Option[UserRecord],
      accesslevel: DocumentAccessLevel
    )(implicit request: RequestHeader) = {
    
    logDocumentView(doc.document, Some(currentPart), accesslevel)
    
    // Needed in any case - start now (val)
    val fCountAnnotations = annotations.countByDocId(doc.id)
    
    // Needed only for Text and TEI - start on demand (def)
    def fReadTextfile() = uploads.readTextfile(doc.ownerName, doc.id, currentPart.getFile) 
      
    ContentType.withName(currentPart.getContentType) match {

      case Some(ContentType.IMAGE_UPLOAD) | Some(ContentType.IMAGE_IIIF) =>
        fCountAnnotations.map(c => Ok(views.html.document.annotation.image(doc, currentPart, loggedInUser, accesslevel, c)))
          
      case Some(ContentType.TEXT_PLAIN) =>
        fReadTextfile() flatMap {
          case Some(content) =>
            fCountAnnotations.map(c => Ok(views.html.document.annotation.text(doc, currentPart, loggedInUser, accesslevel, c, content)))

          case None =>
            // Filepart found in DB, but not file on filesystem
            Logger.error("Filepart recorded in the DB is missing on the filesystem: " + doc.ownerName + ", " + doc.id)
            Future.successful(InternalServerError)
        }
      
      case Some(ContentType.TEXT_TEIXML) =>
        fReadTextfile flatMap {
          case Some(content) =>
            fCountAnnotations.map { c =>
              val preview = previewFromTEI(content)
              Ok(views.html.document.annotation.tei(doc, currentPart, loggedInUser, accesslevel, preview, c))
            }
              
          case None =>
            // Filepart found in DB, but not file on filesystem
            Logger.error("Filepart recorded in the DB is missing on the filesystem: " + doc.ownerName + ", " + doc.id)
            Future.successful(InternalServerError)
        }
        
      case Some(ContentType.DATA_CSV) =>
        fCountAnnotations.map(c => Ok(views.html.document.annotation.table(doc, currentPart, loggedInUser, accesslevel, c)))

      case _ =>
        // Unknown content type in DB, or content type we don't have an annotation view for - should never happen
        Future.successful(InternalServerError)
    }
    
  }

}

package controllers.document.annotation

import controllers.{ BaseOptAuthController, ControllerContext }
import javax.inject.Inject
import models.ContentType
import models.annotation.AnnotationService
import models.document.DocumentAccessLevel
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import play.api.mvc.RequestHeader
import scala.concurrent.Future
import storage.FileAccess

class AnnotationController @Inject() (implicit val ctx: ControllerContext) extends BaseOptAuthController with FileAccess {

  /** Just a redirect for convenience **/
  def showAnnotationViewForDoc(documentId: String) = StackAction { implicit request =>
    Redirect(routes.AnnotationController.showAnnotationViewForDocPart(documentId, 1))
  }

  def showAnnotationViewForDocPart(documentId: String, partNo: Int) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user.getUsername)
    documentPartResponse(documentId, partNo, maybeUser, { case (document, fileparts, selectedPart, accesslevel) =>
      if (accesslevel.canRead)
        renderResponse(maybeUser, document, fileparts, selectedPart, accesslevel)
      else if (loggedIn.isEmpty) // No read rights - but user is not logged in yet 
        authenticationFailed(request)(ctx.ec)        
      else
        Future.successful(ForbiddenPage)
    })
  }

  private def renderResponse(loggedInUser: Option[String], document: DocumentRecord, parts: Seq[DocumentFilepartRecord],
      thisPart: DocumentFilepartRecord, accesslevel: DocumentAccessLevel)(implicit request: RequestHeader) =

    ContentType.withName(thisPart.getContentType) match {

      case Some(ContentType.IMAGE_UPLOAD) =>
        Future.successful(Ok(views.html.document.annotation.image(loggedInUser, document, parts, thisPart, accesslevel)))

      case Some(ContentType.TEXT_PLAIN) => {
        readTextfile(document.getOwner, document.getId, thisPart.getFilename) match {
          case Some(content) => {
            AnnotationService.countByDocId(document.getId).map(documentAnnotationCount =>
              Ok(views.html.document.annotation.text(loggedInUser, document, parts, thisPart, documentAnnotationCount, accesslevel, content)))
          }
          
          case None => {
            // Filepart found in DB, but not file on filesystem
            Logger.error("Filepart recorded in the DB is missing on the filesystem: " + document.getOwner + ", " + document.getId)
            Future.successful(InternalServerError)
          }
        }
      }

      case _ =>
        // Unknown content type in DB, or content type we don't have an annotation view for - should never happen
        Future.successful(InternalServerError)
    }
  
}

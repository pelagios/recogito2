package controllers.document.annotation

import controllers.{ BaseOptAuthController, WebJarAssets }
import javax.inject.Inject
import models.ContentType
import models.annotation.AnnotationService
import models.document.{ DocumentAccessLevel, DocumentInfo, DocumentService }
import models.generated.tables.records.{ DocumentFilepartRecord, DocumentRecord, UserRecord }
import models.user.UserService
import play.api.{ Configuration, Logger }
import play.api.mvc.RequestHeader
import scala.concurrent.{ExecutionContext, Future}
import storage.Uploads

class AnnotationController @Inject() (
    val config: Configuration,
    val annotations: AnnotationService,
    val documents: DocumentService,
    val users: UserService,
    val uploads: Uploads,
    implicit val webjars: WebJarAssets,
    implicit val ctx: ExecutionContext
  ) extends BaseOptAuthController(config, documents, users) {

  /** Just a redirect for convenience **/
  def showAnnotationViewForDoc(documentId: String) = StackAction { implicit request =>
    Redirect(routes.AnnotationController.showAnnotationViewForDocPart(documentId, 1))
  }

  def showAnnotationViewForDocPart(documentId: String, partNo: Int) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user)
    documentPartResponse(documentId, partNo, maybeUser, { case (doc, currentPart, accesslevel) =>
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
    )(implicit request: RequestHeader) =

    ContentType.withName(currentPart.getContentType) match {

      case Some(ContentType.IMAGE_UPLOAD) =>
        Future.successful(Ok(views.html.document.annotation.image(doc, currentPart, loggedInUser, accesslevel)))

      case Some(ContentType.TEXT_PLAIN) =>
        uploads.readTextfile(doc.ownerName, doc.id, currentPart.getFilename) match {
          case Some(content) =>
            annotations.countByDocId(doc.id).map(documentAnnotationCount =>
              Ok(views.html.document.annotation.text(doc, loggedInUser, currentPart, accesslevel, documentAnnotationCount, content)))

          case None =>
            // Filepart found in DB, but not file on filesystem
            Logger.error("Filepart recorded in the DB is missing on the filesystem: " + doc.ownerName + ", " + doc.id)
            Future.successful(InternalServerError)
        }

      case _ =>
        // Unknown content type in DB, or content type we don't have an annotation view for - should never happen
        Future.successful(InternalServerError)
    }

}

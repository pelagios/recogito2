package controllers.document

import controllers.BaseOptAuthController
import java.io.File
import javax.inject.Inject
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import models.user.UserService
import play.api.Configuration
import play.api.mvc.Action
import scala.concurrent.{ ExecutionContext, Future }
import storage.Uploads

class DocumentController @Inject() (
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService,
    val uploads: Uploads,
    implicit val ctx: ExecutionContext
  ) extends BaseOptAuthController(config, documents, users) {

  def initialView(docId: String) = Action {
    Redirect(controllers.document.annotation.routes.AnnotationController.showAnnotationViewForDocPart(docId, 1))
  }

  def getImageManifest(docId: String, partNo: Int) = AsyncStack { implicit request =>

    import models.ContentType._

    val maybeUser = loggedIn.map(_.user)
    documentPartResponse(docId, partNo, maybeUser, { case (document, fileparts, filepart, accesslevel) =>
      val contentType = filepart.getContentType
      val maybeManifestName =
        if (contentType == IMAGE_UPLOAD.toString)
          // All uploads are Zoomify format
          Some("ImageProperties.xml")
        else if (contentType == IMAGE_IIIF.toString)
          None // TODO future feature
        else
          None

      maybeManifestName match {
        case Some(filename) =>
          getTilesetFile(document, filepart, filename).map {
            case Some(file) => Ok.sendFile(file)
            case None => InternalServerError // Document folder doesn't contain a manifset file
          }

        case None => Future.successful(InternalServerError) // Unsupported content type (shouldn't happen)
      }
    })
  }

  /** Common retrieval code for tiles and manifests **/
  private def getTilesetFile(document: DocumentRecord, part: DocumentFilepartRecord, filepath: String): Future[Option[File]] = Future {
    scala.concurrent.blocking {
      // ownerDataDir must exist unless DB integrity broken - outer documentPartResponse will handle failure
      val documentDir = uploads.getDocumentDir(document.getOwner, document.getId).get

      // Tileset foldername is, by convention, equal to filename minus extension
      val foldername = part.getFilename.substring(0, part.getFilename.lastIndexOf('.'))
      val tileFolder = new File(documentDir, foldername)

      val file = new File(tileFolder, filepath)
      if (file.exists)
        Some(file)
      else
        None
    }
  }

  def getImageTile(docId: String, partNo: Int, tilepath: String) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user)
    documentPartResponse(docId, partNo, maybeUser, { case (document, fileparts, filepart, accesslevel) =>
      getTilesetFile(document, filepart, tilepath).map {
        case Some(file) => Ok.sendFile(file)
        case None => NotFound
      }
    })
  }

  def getThumbnail(docId: String, partNo: Int) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user)
    documentPartResponse(docId, partNo, maybeUser, { case (document, fileparts, filepart, accesslevel) =>
      uploads.openThumbnail(document.getOwner, docId, filepart.getFilename).map {
        case Some(file) => Ok.sendFile(file)
        case None => NotFoundPage
      }
    })
  }

}

package controllers.document

import controllers.BaseOptAuthController
import java.io.File
import javax.inject.{ Inject, Singleton }
import models.ContentType
import models.document.DocumentService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import models.user.UserService
import play.api.Configuration
import play.api.http.FileMimeTypes
import play.api.mvc.Action
import scala.concurrent.{ ExecutionContext, Future }
import storage.Uploads
import scala.io.Source

@Singleton
class DocumentController @Inject() (
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService,
    val uploads: Uploads,
    implicit val mimeTypes: FileMimeTypes,
    implicit val ctx: ExecutionContext
  ) extends BaseOptAuthController(config, documents, users) {

  def initialDocumentView(docId: String) = Action {
    Redirect(controllers.document.annotation.routes.AnnotationController.showAnnotationView(docId, 1))
  }
  
  /** Common retrieval code for tiles and manifests **/
  private def getTilesetFile(document: DocumentRecord, part: DocumentFilepartRecord, filepath: String): Future[Option[File]] = Future {
    scala.concurrent.blocking {
      // ownerDataDir must exist unless DB integrity broken - outer documentPartResponse will handle failure
      val documentDir = uploads.getDocumentDir(document.getOwner, document.getId).get

      // Tileset foldername is, by convention, equal to filename minus extension
      val foldername = part.getFile.substring(0, part.getFile.lastIndexOf('.'))
      val tileFolder = new File(documentDir, foldername)

      val file = new File(tileFolder, filepath)
      if (file.exists)
        Some(file)
      else
        None
    }
  }

  /** Gets the image manifest for the given document part.
    * - for uploaded images, returns the Zoomify ImageProperties.xml file
    * - for IIIF, returns a redirect to the original location of the IIIF info.json   
    * - in case the document is not an image, returns a BadRequest   
    */
  def getImageManifest(docId: String, partNo: Int) = AsyncStack { implicit request =>
    documentPartResponse(docId, partNo, loggedIn, { case (doc, currentPart, accesslevel) =>
      ContentType.withName(currentPart.getContentType) match {
        
        case Some(ContentType.IMAGE_UPLOAD) =>
          getTilesetFile(doc.document, currentPart, "ImageProperties.xml").map {
            case Some(file) => Ok.sendFile(file)
            case None => InternalServerError
          }
          
        case Some(ContentType.IMAGE_IIIF) =>
          Future.successful(Redirect(currentPart.getFile))
          
        case _ =>
          Future.successful(BadRequest)
          
      }
    })
  }

  /** Returns the image tile at the given relative file path for the specified document part **/
  def getImageTile(docId: String, partNo: Int, tilepath: String) = AsyncStack { implicit request =>
    documentPartResponse(docId, partNo, loggedIn, { case (doc, currentPart, accesslevel) =>
      getTilesetFile(doc.document, currentPart, tilepath).map {
        case Some(file) => Ok.sendFile(file)
        case None => NotFound
      }
    })
  }

  /** Returns a thumbnail file for the given document part **/
  def getThumbnail(docId: String, partNo: Int) = AsyncStack { implicit request =>
    
    import models.ContentType._
    
    def iiifThumbnailURL(iiifUrl: String) =
      iiifUrl.substring(0, iiifUrl.length - 9) + "full/160,/0/default.jpg"
    
    documentPartResponse(docId, partNo, loggedIn, { case (doc, currentPart, accesslevel) =>
      if (currentPart.getContentType == IMAGE_IIIF.toString) {        
        Future.successful(Redirect(iiifThumbnailURL(currentPart.getFile)))
      } else {
        uploads.openThumbnail(doc.ownerName, docId, currentPart.getFile).map {
          case Some(file) => Ok.sendFile(file)
          case None => NotFound
        }
      }
    })
  }
  
  /** Gets the raw content for the given document part **/
  def getRaw(docId: String, partNo: Int, lines: Option[Int]) = AsyncStack { implicit request =>
    documentPartResponse(docId, partNo, loggedIn, { case (document, currentPart, accesslevel) =>
      Future {
        scala.concurrent.blocking {
          val documentDir = uploads.getDocumentDir(document.owner.getUsername, document.id).get
          val file = new File(documentDir, currentPart.getFile)
          if (file.exists)
            lines match {
              case Some(limit) =>
                Ok(Source.fromFile(file).getLines().take(limit).mkString("\n"))
                  .as("text/csv")
                  .withHeaders(CONTENT_DISPOSITION -> { "attachment; filename=" + currentPart.getId + "." + limit + ".csv" })
                
              case None =>
                Ok.sendFile(file)
            }
          else
            NotFound
        }
      }
    })
  }

}

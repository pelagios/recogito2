package controllers.document

import controllers.BaseController
import java.io.File
import javax.inject.Inject
import models.document.DocumentService
import models.user.Roles._
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import storage.{ DB, FileAccess }

class DocumentController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController with FileAccess {
  
  def getImageTile(docId: String, partNo: Int, tilepath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentPartResponse(docId, partNo, loggedIn.user.getUsername, { case (document, fileparts, filepart) =>
      // ownerDataDir must exist, unless DB integrity is broken - renderDocumentResponse will handle the exception if .get fails
      val documentDir = getDocumentDir(document.getOwner, document.getId).get
      
      // Tileset foldername is, by convention, equal to filename minus extension
      val foldername = filepart.getFilename.substring(0, filepart.getFilename.lastIndexOf('.'))
      val tileFolder = new File(documentDir, foldername)
      
      val file = new File(tileFolder, tilepath)
      if (file.exists)
        Ok.sendFile(file)
      else
        NotFound
    })
  }
  
  def getThumbnail(docId: String, partNo: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentPartResponse(docId, partNo, loggedIn.user.getUsername, { case (document, fileparts, filepart) =>
      openThumbnail(loggedIn.user.getUsername, docId, filepart.getFilename) match {
        case Some(file) => Ok.sendFile(file)
        case None => NotFound
      }
    })
  }
  
  def deleteDocument(docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    DocumentService.findById(docId).flatMap(_ match {
      case Some(document) => {
        // Only the owner can delete a document
        if (document.getOwner == loggedIn.user.getUsername)
          DocumentService.delete(document).map(_ => Status(200))
        else
          Future.successful(Forbidden)
      }

      case None =>
        // No document with that ID found in DB
        Future.successful(NotFound)
    }).recover { case t =>
      t.printStackTrace()
      InternalServerError(t.getMessage)
    }
  }
  
}
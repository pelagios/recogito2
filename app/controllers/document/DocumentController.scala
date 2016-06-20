package controllers.document

import controllers.BaseController
import java.io.File
import javax.inject.Inject
import models.annotation.AnnotationService
import models.document.{ DocumentService, PartOrdering }
import models.user.Roles._
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import storage.{ DB, FileAccess }
import models.document.DocumentAccessLevel

class DocumentController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController with FileAccess {
    
  def getImageTile(docId: String, partNo: Int, tilepath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentPartResponse(docId, partNo, loggedIn.user.getUsername, { case (document, fileparts, filepart, accesslevel) =>
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
    renderDocumentPartResponse(docId, partNo, loggedIn.user.getUsername, { case (document, fileparts, filepart, accesslevel) =>
      openThumbnail(loggedIn.user.getUsername, docId, filepart.getFilename) match {
        case Some(file) => Ok.sendFile(file)
        case None => NotFound
      }
    })
  }
  
  def deleteDocument(docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    DocumentService.findById(docId, Some(loggedIn.user.getUsername)).flatMap(_ match {
      case Some((document, accesslevel)) => {
        if (accesslevel == DocumentAccessLevel.OWNER) // We allow only the owner to delete a document
          for {
            _ <- DocumentService.delete(document)
            success <- AnnotationService.deleteByDocId(docId)
          } yield if (success) Status(200) else InternalServerError
        else
          Future.successful(Forbidden)
      }

      case None =>
        Future.successful(NotFound) // No document with that ID found in DB
    }).recover { case t =>
      t.printStackTrace()
      InternalServerError(t.getMessage)
    }
  }
  
}
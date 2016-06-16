package controllers.document

import controllers.BaseController
import java.io.File
import javax.inject.Inject
import models.annotation.AnnotationService
import models.document.{ DocumentService, PartOrdering }
import models.user.Roles._
import play.api.cache.CacheApi
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import storage.{ DB, FileAccess }
import models.document.DocumentAccessLevel

class DocumentController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController with FileAccess {
  
  implicit val orderingReads: Reads[PartOrdering] = (
    (JsPath \ "id").read[Int] and
    (JsPath \ "sequence_no").read[Int]
  )(PartOrdering.apply _)
  
  def setSortOrder(docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    request.body.asJson match {
      case Some(json) => Json.fromJson[Seq[PartOrdering]](json) match {

        case s: JsSuccess[Seq[PartOrdering]] =>
          // TODO verify the user has right access on this document
          DocumentService.setFilepartSortOrder(docId, s.get).map(_ => Status(200))
        
        case e: JsError => 
          Future.successful(BadRequest)
        
      }
        
      case None =>
        Future.successful(BadRequest)
    }    
  }

  /*
  def setIsVisibleToPublic(docId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>

  }
  */
  
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
      case Some((document, accesslevel)) => {
        // Only the owner is allowed to delete a document
        if (accesslevel == DocumentAccessLevel.OWNER)
          for {
            _ <- DocumentService.delete(document)
            success <- AnnotationService.deleteByDocId(docId)
          } yield if (success) Status(200) else InternalServerError
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
package controllers.document

import controllers.BaseAuthController
import java.io.File
import javax.inject.Inject
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import play.api.Configuration
import play.api.mvc.Action
import scala.concurrent.ExecutionContext
import storage.Uploads

class DocumentController @Inject() (
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService, 
    val uploads: Uploads,
    implicit val ctx: ExecutionContext
  ) extends BaseAuthController(config, documents, users) {
    
  def initialView(docId: String) = Action {
    Redirect(controllers.document.annotation.routes.AnnotationController.showAnnotationViewForDocPart(docId, 1))
  }
  
  def getImageTile(docId: String, partNo: Int, tilepath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentPartResponse(docId, partNo, loggedIn.user.getUsername, { case (document, fileparts, filepart, accesslevel) =>
      // ownerDataDir must exist, unless DB integrity is broken - renderDocumentResponse will handle the exception if .get fails
      val documentDir = uploads.getDocumentDir(document.getOwner, document.getId).get
      
      // Tileset foldername is, by convention, equal to filename minus extension
      val foldername = filepart.getFilename.substring(0, filepart.getFilename.lastIndexOf('.'))
      val tileFolder = new File(documentDir, foldername)
      
      val file = new File(tileFolder, tilepath)
      if (file.exists)
        Ok.sendFile(file)
      else
        NotFoundPage
    })
  }
  
  def getThumbnail(docId: String, partNo: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentPartResponse(docId, partNo, loggedIn.user.getUsername, { case (document, fileparts, filepart, accesslevel) =>
      uploads.openThumbnail(loggedIn.user.getUsername, docId, filepart.getFilename) match {
        case Some(file) => Ok.sendFile(file)
        case None => NotFoundPage
      }
    })
  }
  
}
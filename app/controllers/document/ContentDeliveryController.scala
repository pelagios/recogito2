package controllers.document

import controllers.{ AbstractController, Security }
import java.io.File
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.user.Roles._
import storage.{ DB, FileAccess }

import play.api.Logger

class ContentDeliveryController @Inject() (implicit val db: DB) extends AbstractController with AuthElement with Security with FileAccess {
  
  def getImageTile(docId: String, partNo: Int, tilepath: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentPartResponse(docId, partNo, loggedIn.getUsername, { case (document, fileparts, filepart) =>
      // ownerDataDir must exist, unless DB integrity is broken - renderDocumentResponse will handle the exception if .get fails
      val ownerDataDir = getUserDir(document.getOwner).get
      
      // Tileset foldername is, by convention, equal to filename minus extension
      val foldername = filepart.getFilename.substring(0, filepart.getFilename.lastIndexOf('.'))
      val tileFolder = new File(ownerDataDir, foldername)
      
      val file = new File(tileFolder, tilepath)
      if (file.exists)
        Ok.sendFile(file)
      else
        NotFound
    })
  }
  
}
package controllers.my.downloads

import controllers.{ AbstractController, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.user.Roles._
import storage.DB

class DownloadsController @Inject() (implicit val db: DB) extends AbstractController with AuthElement with Security {

  def showDownloadOptions(documentId: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentResponse(documentId, loggedIn.getUsername, 
        { case (document, fileparts) =>  Ok(views.html.my.downloads.index(document)) })
  }

}

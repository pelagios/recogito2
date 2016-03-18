package controllers.document.downloads

import controllers.BaseController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class DownloadsController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {

  def showDownloadOptions(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentResponse(documentId, loggedIn.getUsername,
        { case (document, fileparts) =>  Ok(views.html.document.downloads.index(loggedIn.getUsername, document)) })
  }

}

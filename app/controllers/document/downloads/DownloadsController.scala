package controllers.document.downloads

import controllers.BaseAuthController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class DownloadsController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseAuthController {

  /** TODO this view should be available without login, if the document is set to public **/
  def showDownloadOptions(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentResponse(documentId, loggedIn.user.getUsername,
        { case (document, fileparts, accesslevel) =>  Ok(views.html.document.downloads.index(Some(loggedIn.user.getUsername), document, accesslevel)) })
  }

}

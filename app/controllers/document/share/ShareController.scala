package controllers.document.share

import controllers.BaseAuthController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class ShareController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseAuthController {

  def showShareSettings(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentResponse(documentId, loggedIn.user.getUsername,
        { case (document, fileparts, accesslevel) =>  Ok(views.html.document.share.index(loggedIn.user.getUsername, document, accesslevel)) })
  }

}

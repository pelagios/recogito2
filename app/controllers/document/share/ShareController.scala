package controllers.document.share

import controllers.AbstractController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class ShareController @Inject() (implicit val cache: CacheApi, val db: DB) extends AbstractController {

  def showShareSettings(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentResponse(documentId, loggedIn.getUsername,
        { case (document, fileparts) =>  Ok(views.html.document.share.index(loggedIn.getUsername, document)) })
  }

}

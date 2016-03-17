package controllers.document.settings

import controllers.AbstractController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class SettingsController @Inject() (implicit val cache: CacheApi, val db: DB) extends AbstractController {

  def showDocumentSettings(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentResponse(documentId, loggedIn.getUsername,
        { case (document, fileparts) =>  Ok(views.html.document.settings.index(loggedIn.getUsername, document)) })
  }

}

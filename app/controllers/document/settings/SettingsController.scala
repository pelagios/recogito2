package controllers.document.settings

import controllers.BaseController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class SettingsController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {

  def showDocumentSettings(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentResponse(documentId, loggedIn.user.getUsername,
        { case (document, fileparts) =>  Ok(views.html.document.settings.index(loggedIn.user.getUsername, document)) })
  }

}

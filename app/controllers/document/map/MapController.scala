package controllers.document.map

import controllers.BaseController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class MapController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseController {

  def showMap(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentResponse(documentId, loggedIn.user.getUsername,
        { case (document, fileparts, accesslevel) =>  Ok(views.html.document.map.index(loggedIn.user.getUsername, document)) })
  }

}

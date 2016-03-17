package controllers.document.map

import controllers.AbstractController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class MapController @Inject() (implicit val cache: CacheApi, val db: DB) extends AbstractController {

  def showMap(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentResponse(documentId, loggedIn.getUsername,
        { case (document, fileparts) =>  Ok(views.html.document.map.index(loggedIn.getUsername, document)) })
  }

}

package controllers.document.map

import controllers.{ BaseAuthController, WebJarAssets }
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class MapController @Inject() (implicit val cache: CacheApi, val db: DB, webjars: WebJarAssets) extends BaseAuthController {

  /** TODO this view should be available without login, if the document is set to public **/
  def showMap(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentResponse(documentId, loggedIn.user.getUsername,
        { case (document, fileparts, accesslevel) =>  Ok(views.html.document.map.index(Some(loggedIn.user.getUsername), document, accesslevel)) })
  }

}

package controllers.document.stats

import controllers.BaseAuthController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class StatsController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseAuthController {
  
  /** TODO this view should be available without login, if the document is set to public **/
  def showDocumentStats(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentResponse(documentId, loggedIn.user.getUsername,
        { case (document, fileparts, accesslevel) =>  Ok(views.html.document.stats.index(Some(loggedIn.user.getUsername), document, accesslevel)) })
  }

}

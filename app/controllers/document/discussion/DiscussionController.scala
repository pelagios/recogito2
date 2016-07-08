package controllers.document.discussion

import controllers.BaseAuthController
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import storage.DB

class DiscussionController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseAuthController {

  def showDiscussionBoard(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentResponse(documentId, loggedIn.user.getUsername,
        { case (document, fileparts, accesslevel) =>  Ok(views.html.document.discussion.index(loggedIn.user.getUsername, document, accesslevel)) })
  }

}

package controllers.document.stats

import controllers.{ AbstractController, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.AuthElement
import models.user.Roles._
import storage.DB

class StatsController @Inject() (implicit val db: DB) extends AbstractController with AuthElement with Security {

  def showDocumentStats(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    renderDocumentResponse(documentId, loggedIn.getUsername,
        { case (document, fileparts) =>  Ok(views.html.document.stats.index(loggedIn.getUsername, document)) })
  }

}

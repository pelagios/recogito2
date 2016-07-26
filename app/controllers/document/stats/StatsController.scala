package controllers.document.stats

import controllers.{ ControllerContext, BaseAuthController }
import javax.inject.Inject
import models.user.Roles._

class StatsController @Inject() (implicit val ctx: ControllerContext) extends BaseAuthController {
  
  /** TODO this view should be available without login, if the document is set to public **/
  def showDocumentStats(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentResponse(documentId, loggedIn.user.getUsername,
        { case (document, fileparts, accesslevel) =>  Ok(views.html.document.stats.index(Some(loggedIn.user.getUsername), document, accesslevel)) })
  }

}

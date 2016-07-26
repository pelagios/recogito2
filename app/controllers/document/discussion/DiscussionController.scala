package controllers.document.discussion

import controllers.{ BaseAuthController, ControllerContext }
import javax.inject.Inject
import models.user.Roles._

class DiscussionController @Inject() (implicit val ctx: ControllerContext) extends BaseAuthController {

  def showDiscussionBoard(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentResponse(documentId, loggedIn.user.getUsername,
        { case (document, fileparts, accesslevel) =>  Ok(views.html.document.discussion.index(loggedIn.user.getUsername, document, accesslevel)) })
  }

}

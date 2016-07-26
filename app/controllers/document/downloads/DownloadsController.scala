package controllers.document.downloads

import controllers.{ BaseAuthController, ControllerContext }
import javax.inject.Inject
import models.user.Roles._

class DownloadsController @Inject() (implicit val ctx: ControllerContext) extends BaseAuthController {

  /** TODO this view should be available without login, if the document is set to public **/
  def showDownloadOptions(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentResponse(documentId, loggedIn.user.getUsername,
        { case (document, fileparts, accesslevel) =>  Ok(views.html.document.downloads.index(Some(loggedIn.user.getUsername), document, accesslevel)) })
  }

}

package controllers.document.map

import controllers.{ BaseOptAuthController, ControllerContext }
import javax.inject.Inject
import models.user.Roles._
import scala.concurrent.Future

class MapController @Inject() (implicit val ctx: ControllerContext) extends BaseOptAuthController {

  /** TODO this view should be available without login, if the document is set to public **/
  def showMap(documentId: String) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user.getUsername)
    documentResponse(documentId, maybeUser, { case (document, fileparts, accesslevel) =>
      if (accesslevel.canRead)
        Future.successful(Ok(views.html.document.map.index(maybeUser, document, accesslevel)))
      else if (loggedIn.isEmpty) // No read rights - but user is not logged in yet 
        authenticationFailed(request)        
      else
        Future.successful(ForbiddenPage)
    })
  }

}

package controllers.document.map

import controllers.{ BaseOptAuthController, WebJarAssets }
import javax.inject.Inject
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import play.api.Configuration
import scala.concurrent.{ ExecutionContext, Future }
import controllers.WebJarAssets

class MapController @Inject() (
    val config: Configuration,
    val document: DocumentService,
    val users: UserService,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarAssets
  ) extends BaseOptAuthController(config, document, users) {

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

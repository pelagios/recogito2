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

  def showMap(documentId: String) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user.getUsername)
    
    documentReadResponse(documentId, maybeUser,  { case (document, fileparts, accesslevel) =>
      Future.successful(Ok(views.html.document.map.index(maybeUser, document, accesslevel)))
    })
  }

}

package controllers.document.map

import controllers.{ BaseOptAuthController, HasVisitLogging, WebJarAssets }
import javax.inject.Inject
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import models.visit.VisitService
import play.api.Configuration
import scala.concurrent.{ ExecutionContext, Future }
import controllers.WebJarAssets

class MapController @Inject() (
    val config: Configuration,
    val document: DocumentService,
    val users: UserService,
    implicit val visitService: VisitService,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarAssets
  ) extends BaseOptAuthController(config, document, users) with HasVisitLogging {

  def showMap(documentId: String) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user)
    
    documentReadResponse(documentId, maybeUser,  { case (doc, accesslevel) =>
      logDocumentView(doc.document, None, accesslevel)
      Future.successful(Ok(views.html.document.map.index(doc, maybeUser, accesslevel)))
    })
  }

}

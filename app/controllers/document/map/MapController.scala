package controllers.document.map

import controllers.{ BaseOptAuthController, HasVisitLogging }
import javax.inject.{ Inject, Singleton }
import models.document.DocumentService
import models.annotation.AnnotationService
import models.user.UserService
import models.user.Roles._
import models.visit.VisitService
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import scala.concurrent.{ ExecutionContext, Future }
import controllers.WebJarAssets

@Singleton
class MapController @Inject() (
    val config: Configuration,
    val annotations: AnnotationService,
    val document: DocumentService,
    val users: UserService,
    implicit val visitService: VisitService,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarsUtil
  ) extends BaseOptAuthController(config, document, users) with HasVisitLogging {

  def showMap(documentId: String) = AsyncStack { implicit request =>
    documentReadResponse(documentId, loggedIn,  { case (doc, accesslevel) =>
      logDocumentView(doc.document, None, accesslevel)
      annotations.countByDocId(documentId).map { documentAnnotationCount =>
        Ok(views.html.document.map.index(doc, loggedIn, accesslevel, documentAnnotationCount))
      }
    })
  }

}

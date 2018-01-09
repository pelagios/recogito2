package controllers.document.map

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseOptAuthController, HasVisitLogging, Security}
import javax.inject.{Inject, Singleton}
import models.document.DocumentService
import models.annotation.AnnotationService
import models.user.UserService
import models.user.Roles._
import models.visit.VisitService
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import controllers.WebJarAssets

@Singleton
class MapController @Inject() (
    val components: ControllerComponents,
    val config: Configuration,
    val annotations: AnnotationService,
    val document: DocumentService,
    val users: UserService,
    val silhouette: Silhouette[Security.Env],
    implicit val visitService: VisitService,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarsUtil
  ) extends BaseOptAuthController(components, config, document, users) with HasVisitLogging {

  def showMap(documentId: String) = silhouette.UserAwareAction.async { implicit request =>
    documentReadResponse(documentId, request.identity,  { case (doc, accesslevel) =>
      logDocumentView(doc.document, None, accesslevel)
      annotations.countByDocId(documentId).map { documentAnnotationCount =>
        Ok(views.html.document.map.index(doc, request.identity, accesslevel, documentAnnotationCount))
      }
    })
  }

}

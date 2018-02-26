package controllers.document.map

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseOptAuthController, HasVisitLogging, Security}
import javax.inject.{Inject, Singleton}
import services.document.DocumentService
import services.annotation.AnnotationService
import services.user.UserService
import services.user.Roles._
import services.visit.VisitService
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.ControllerComponents
import play.api.i18n.I18nSupport
import scala.concurrent.{ExecutionContext, Future}

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
  ) extends BaseOptAuthController(components, config, document, users) with HasVisitLogging with I18nSupport {

  def showMap(documentId: String) = silhouette.UserAwareAction.async { implicit request =>
    documentReadResponse(documentId, request.identity,  { case (doc, accesslevel) =>
      logDocumentView(doc.document, None, accesslevel)
      annotations.countByDocId(documentId).map { documentAnnotationCount =>
        Ok(views.html.document.map.index(doc, request.identity, accesslevel, documentAnnotationCount))
      }
    })
  }

}

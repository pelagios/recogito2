package controllers.document.stats

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import javax.inject.{Inject, Singleton}
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext

@Singleton
class StatsController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val webjars: WebJarsUtil,
  implicit val ctx: ExecutionContext
) extends BaseAuthController(components, config, documents, users) {

  /** TODO this view should be available without login, if the document is set to public **/
  def showDocumentStats(documentId: String) = silhouette.SecuredAction.async { implicit request =>
    documentResponse(documentId, request.identity,{ case (doc, accesslevel) => 
      Ok(views.html.document.stats.index(doc, Some(request.identity), accesslevel)) })
  }

}

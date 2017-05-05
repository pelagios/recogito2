package controllers.document.stats

import controllers.{ BaseAuthController, WebJarAssets }
import javax.inject.{ Inject, Singleton }
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import play.api.Configuration
import scala.concurrent.ExecutionContext

@Singleton
class StatsController @Inject() (
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService,
    implicit val webjars: WebJarAssets,
    implicit val ctx: ExecutionContext
  ) extends BaseAuthController(config, documents, users) {

  /** TODO this view should be available without login, if the document is set to public **/
  def showDocumentStats(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentResponse(documentId, loggedIn.user,{ case (doc, accesslevel) => 
      Ok(views.html.document.stats.index(doc, Some(loggedIn.user), accesslevel)) })
  }

}

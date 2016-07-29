package controllers.document.discussion

import controllers.{ BaseAuthController, WebJarAssets }
import javax.inject.Inject
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import play.api.Configuration
import scala.concurrent.ExecutionContext

class DiscussionController @Inject() (
    val config: Configuration,
    documents: DocumentService,
    val users: UserService,
    implicit val webjars: WebJarAssets,
    implicit val ctx: ExecutionContext
  ) extends BaseAuthController(config, documents, users) {

  def showDiscussionBoard(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentResponse(documentId, loggedIn.user.getUsername,
        { case (document, fileparts, accesslevel) =>  Ok(views.html.document.discussion.index(loggedIn.user.getUsername, document, accesslevel)) })
  }

}

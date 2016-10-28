package controllers.admin.users

import controllers.BaseAuthController
import javax.inject.Inject
import models.document.DocumentService
import models.user.Roles._
import models.user.UserService
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import controllers.WebJarAssets
import play.api.Configuration

class UserAdminController @Inject() (
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService,
    implicit val ctx: ExecutionContext,
    implicit val webjars: WebJarAssets
  ) extends BaseAuthController(config, documents, users) {

  def index = StackAction(AuthorityKey -> Admin) { implicit request =>
    Ok(views.html.admin.users.index())
  }

  def showDetails(username: String) = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    users.findByUsername(username).flatMap(_ match {

      case Some(user) =>
        documents.findByOwner(username).map(documents =>
          Ok(views.html.admin.users.details(user, documents))) 

      case None => Future.successful(NotFoundPage)

    })
  }

}

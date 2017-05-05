package controllers.admin

import controllers.{ BaseAuthController, WebJarAssets }
import javax.inject.{ Inject, Singleton }
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import play.api.Configuration

@Singleton
class AdminController @Inject() (
    val config: Configuration,
    val documents: DocumentService,
    val users: UserService,
    implicit val webjars: WebJarAssets
  ) extends BaseAuthController(config, documents, users) {
        
  def index = StackAction(AuthorityKey -> Admin) { implicit request =>
    Ok(views.html.admin.index())
  }
  
}
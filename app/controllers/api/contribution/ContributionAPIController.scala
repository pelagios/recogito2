package controllers.api.contribution

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security, HasPrettyPrintJSON}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.ControllerComponents 
import services.document.DocumentService
import services.user.UserService

@Singleton
class ContributionAPIController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val users: UserService,
  val silhouette: Silhouette[Security.Env]
) extends BaseAuthController(components, config, documents, users) with HasPrettyPrintJSON {

  def getDocumentStats(id: String) = silhouette.SecuredAction.async { implicit request => 
    ???
  }
  
}

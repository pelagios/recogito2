package controllers.api.search

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseOptAuthController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.ControllerComponents
import services.document.DocumentService
import services.user.UserService

@Singleton
class SearchAPIController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val users: UserService,
  val silhouette: Silhouette[Security.Env]
) extends BaseOptAuthController(components, config, documents, users) with HasPrettyPrintJSON {


  /** Search all of public Recogito, plus my own accessible documents **/
  def searchAll = silhouette.UserAwareAction.async { implicit request =>
    ???
  }

  /** Searches just my own documents **/
  def searchMy = silhouette.SecuredAction.async { implicit request => 
    ???
  }

  /** Searches just the documents shared with me **/
  def searchSharedWithMe = silhouette.SecuredAction.async { implicit request =>
    ???
  }

}
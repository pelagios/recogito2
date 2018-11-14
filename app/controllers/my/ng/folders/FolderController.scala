package controllers.my.ng.folders

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import services.user.UserService

@Singleton
class FolderController @Inject() (
    val components: ControllerComponents,
    val silhouette: Silhouette[Security.Env],
    val users: UserService,
    val config: Configuration,
    implicit val ctx: ExecutionContext
  ) extends BaseController(components, config, users)
      with HasPrettyPrintJSON {

  def createFolder(title: String) = silhouette.SecuredAction.async { implicit request => ??? }

  def deleteFolder(id: Int) = silhouette.SecuredAction.async { implicit request => ??? }

  def bulkDeleteFolders = silhouette.SecuredAction.async { implicit request => ??? }

  def getMyFolders(offset: Int, limit: Int) = silhouette.SecuredAction.async { implicit request => ??? }

  def getSharedWithMe(offset: Int, limit: Int) = silhouette.SecuredAction.async { implicit request => ??? }

  def getAccessibleFolders(owner: String, offset: Int, limit: Int) = silhouette.SecuredAction.async { implicit request => ??? }

}
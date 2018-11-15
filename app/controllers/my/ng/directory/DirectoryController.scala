package controllers.my.ng.directory

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import services.user.UserService

@Singleton
class DirectoryController @Inject() (
    val components: ControllerComponents,
    val silhouette: Silhouette[Security.Env],
    val users: UserService,
    val config: Configuration,
    implicit val ctx: ExecutionContext
  ) extends BaseController(components, config, users)
      with HasPrettyPrintJSON {

  /** Directory listing **/
  def getMyDirectory(offset: Int, size: Int, folderId: UUID) =
    silhouette.SecuredAction.async { implicit request => ??? }

  def getSharedWithMe(offset: Int, size: Int, folderId: UUID) =
    silhouette.SecuredAction.async { implicit request => ??? }

  def getAccessibleDocuments(owner: String, offset: Int, size: Int, folderId: UUID) =
    silhouette.SecuredAction.async { implicit request => ??? }

  /** Delete **/
  def deleteDocument(id: String) =
    silhouette.SecuredAction.async { implicit request => ??? }

  def bulkDeleteDocuments() =
    silhouette.SecuredAction.async { implicit request => ??? }

  def deleteFolder(id: UUID) =
    silhouette.SecuredAction.async { implicit request => ??? }

  def bulkDeleteFolders() =
    silhouette.SecuredAction.async { implicit request => ??? }

  /** Unshare **/

  def unshareDocument(id: String) =
    silhouette.SecuredAction.async { implicit request => ??? }

  def bulkUnshareDocuments() =
    silhouette.SecuredAction.async { implicit request => ??? }

  def unshareFolder(id: UUID) =
    silhouette.SecuredAction.async { implicit request => ??? }

  def bulkUnshareFolders() =
    silhouette.SecuredAction.async { implicit request => ??? }

}

package controllers.my.ng.account

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import scala.concurrent.{Future, ExecutionContext}
import services.HasDate
import services.document.DocumentService
import services.user.UserService
import storage.uploads.Uploads

/** A quick hack for local testing of the new React UI **/
@Singleton
class AccountInfoController @Inject() (
    val components: ControllerComponents,
    val documents: DocumentService,
    val silhouette: Silhouette[Security.Env],
    val users: UserService,
    val uploads: Uploads,
    val config: Configuration,
    implicit val ctx: ExecutionContext
  ) extends BaseController(components, config, users)
      with HasPrettyPrintJSON 
      with HasDate {

  /** Sometimes all you want to know is your current login status **/
  def me = silhouette.UserAwareAction { implicit request => 
    val status = request.identity match {
      case Some(user) => LoginStatus.as(user)
      case None => LoginStatus.NOT_LOGGED_IN
    }

    jsonOk(Json.toJson(status))
  }

  /** Returns the personal info about your own account **/
  def getPersonalAccountInfo = silhouette.SecuredAction.async { implicit request =>
    val username = request.identity.username

    val fUser = users.findByUsername(username)
    val fMyDocCount = documents.countByOwner(username)
    val fSharedCount = documents.countBySharedWith(username)
    val fUsedMb = Future(uploads.getUsedDiskspaceKB(username)).map { used =>
      Math.round(100 * used / 1024).toDouble / 100
    }

    val f = for {
      user <- fUser
      myDocCount <- fMyDocCount
      sharedCount <- fSharedCount
      usedMb <- fUsedMb
    } yield (user.get, myDocCount, sharedCount, usedMb)

    f.map { case (user, myDocs, shared, usedMb) =>
      val info = PersonalAccountInfo(user, myDocs, shared, usedMb)
      jsonOk(Json.toJson(info))
    }
  }  

  /** Returns publicly available info about someone else's account **/
  def getVisitedAccountInfo(username: String) = silhouette.UserAwareAction.async { implicit request =>
    users.findByUsernameIgnoreCase(username).flatMap  { _ match {
      case Some(user) =>
        val loggedInAs = request.identity.map(_.username)
        documents.countAccessibleDocuments(user.username, loggedInAs).map { docs =>
          jsonOk(Json.toJson(VisitedAccountInfo(user, docs)))
        }
        
      case None => Future.successful(JSON_NOT_FOUND)
    }}
  }

}
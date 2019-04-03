package controllers.my.account

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import scala.concurrent.{Future, ExecutionContext}
import services.contribution.ContributionService
import services.document.DocumentService
import services.user.UserService
import storage.uploads.Uploads

@Singleton
class AccountInfoController @Inject() (
    val components: ControllerComponents,
    val contributions: ContributionService,
    val documents: DocumentService,
    val silhouette: Silhouette[Security.Env],
    val users: UserService,
    val uploads: Uploads,
    val config: Configuration,
    implicit val ctx: ExecutionContext
  ) extends BaseController(components, config, users)
      with HasPrettyPrintJSON {

  /** Sometimes all you want to know is your current login status **/
  def me = silhouette.UserAwareAction { implicit request => 
    val status = request.identity match {
      case Some(user) => LoginStatus.as(user)
      case None => LoginStatus.NOT_LOGGED_IN
    }

    jsonOk(Json.toJson(status))
  }

  /** Returns the personal info about your own account **/
  def getPrivateAccountInfo = silhouette.SecuredAction.async { implicit request =>
    val username = request.identity.username

    val fUser = users.findByUsername(username)
    val fMyDocCount = documents.countAllByOwner(username)
    val fSharedCount = documents.countDocumentsSharedWithMe(username)
    val fContributorStats = contributions.getContributorStats(username)
    val fUsedMb = Future(uploads.getUsedDiskspaceKB(username)).map { used =>
      Math.round(100 * used / 1024).toDouble / 100
    }
    
    val f = for {
      user <- fUser
      myDocCount <- fMyDocCount
      sharedCount <- fSharedCount
      stats <- fContributorStats
      usedMb <- fUsedMb
    } yield (user.get, myDocCount, sharedCount, stats, usedMb)

    f.map { case (user, myDocs, shared, stats, usedMb) =>
      val info = PrivateAccountInfo(user, myDocs, shared, stats, usedMb)
      jsonOk(Json.toJson(info))
    }
  }  

  /** Returns publicly available info about someone else's account **/
  def getPublicAccountInfo(username: String) = silhouette.UserAwareAction.async { implicit request =>
    users.findByUsernameIgnoreCase(username).flatMap  { _ match {
      case Some(user) =>
        val loggedInAs = request.identity.map(_.username)

        val fAccessibleDocs = documents.countAllAccessibleDocuments(user.username, loggedInAs)
        val fContributorStats = contributions.getContributorStats(user.username)

        val f = for {
          docs <- fAccessibleDocs
          stats <- fContributorStats
        } yield (docs, stats)

        f.map { case (docs, stats) =>
          jsonOk(Json.toJson(PublicAccountInfo(user, docs, stats)))
        }
        
      case None => Future.successful(JSON_NOT_FOUND)
    }}
  }

  /** Returns the top collaborators for this user **/
  def getCollaborators(username: String) = Action.async { implicit request =>
    contributions.getTopCollaborators(username).map { tuples => 
      val json = tuples.map(t => Json.obj("username" -> t._1, "edits" -> t._2)) 
      jsonOk(Json.toJson(json))
    }    
  }

}
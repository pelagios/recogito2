package controllers.admin

import controllers.BaseAuthController
import javax.inject.{ Inject, Singleton }
import models.annotation.AnnotationService
import models.contribution.ContributionService
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import models.visit.VisitService
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.Json
import scala.concurrent.ExecutionContextExecutor
import controllers.HasPrettyPrintJSON
import com.mohiva.play.silhouette.api.Environment
import models.user.User
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator

@Singleton
class AdminController @Inject() (
    val config: Configuration,
    val annotations: AnnotationService,
    val contributions: ContributionService,
    val documents: DocumentService,
    val users: UserService,
    val visits: VisitService,
    implicit val ctx: ExecutionContextExecutor,
    implicit val webJarsUtil: WebJarsUtil
  ) extends BaseAuthController(config, documents, users) with HasPrettyPrintJSON {
        
  def index = play.api.mvc.Action { Ok } /* StackAction(AuthorityKey -> Admin) { implicit request =>
    Ok(views.html.admin.index())
  } */
  
  def getStats() = play.api.mvc.Action { Ok } /* AsyncStack(AuthorityKey -> Admin) { implicit request =>
    
    // DocumentRecord JSON serialization
    import DocumentService._
    
    val fRecentContributions = contributions.getMostRecent(10)
    val fContributionStats = contributions.getGlobalStats()
    val fTotalAnnotations = annotations.countTotal()
    val fTotalVisits = visits.countTotal()
    val fTotalUsers = users.countUsers()

    val f = for {
      recentContributions <- fRecentContributions
      recentAffectedDocuments <- documents.findByIds(recentContributions.map(_.affectsItem.documentId))
      stats <- fContributionStats
      annotationCount <- fTotalAnnotations
      visitCount <- fTotalVisits
      userCount <- fTotalUsers
    } yield (recentContributions, recentAffectedDocuments, stats, annotationCount, visitCount, userCount)
    
    f.map { case (recentContributions, recentAffectedDocuments, stats, annotationCount, visitCount, userCount) =>
      val response =
        Json.obj(
          "recent_contributions" -> recentContributions,
          "recent_documents" -> recentAffectedDocuments,
          "contribution_stats" -> stats,
          "total_annotations" -> annotationCount,
          "total_visits" -> visitCount,
          "total_users" -> userCount)

      jsonOk(response)
    }
  } */
  
}
package controllers.admin

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import javax.inject.{Inject, Singleton}
import services.annotation.AnnotationService
import services.contribution.ContributionService
import services.document.DocumentService
import services.user.UserService
import services.user.Roles._
import services.visit.VisitService
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContextExecutor
import controllers.HasPrettyPrintJSON

@Singleton
class AdminController @Inject() (
  val components: ControllerComponents, 
  val config: Configuration,
  val annotations: AnnotationService,
  val contributions: ContributionService,
  val documents: DocumentService,
  val users: UserService,
  val visits: VisitService,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContextExecutor,
  implicit val webJarsUtil: WebJarsUtil
) extends BaseAuthController(components, config, documents, users) with HasPrettyPrintJSON {
        
  def index = silhouette.SecuredAction(Security.WithRole(Admin)) { implicit request =>
    Ok(views.html.admin.index())
  }
  
  def getStats() = silhouette.SecuredAction(Security.WithRole(Admin)).async { implicit request =>
    
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
  }
  
}
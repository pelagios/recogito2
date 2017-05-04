package controllers.api

import controllers.{ BaseAuthController, HasPrettyPrintJSON }
import javax.inject.Inject
import models.annotation.AnnotationService
import models.contribution.ContributionService
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import models.visit.VisitService
import play.api.Configuration
import play.api.mvc.Action
import play.api.libs.json.{ Json, JsObject }
import scala.concurrent.ExecutionContext

class StatsAPIController @Inject() (
    val config: Configuration,
    val annotations: AnnotationService,
    val contributions: ContributionService,
    val documents: DocumentService,
    val visits: VisitService,
    val users: UserService,
    implicit val ctx: ExecutionContext
  ) extends BaseAuthController(config, documents, users) with HasPrettyPrintJSON {

  // TODO should this be in the settings controller?
  def getContributionHistory(documentId: String, offset: Int, size: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    contributions.getHistory(documentId, offset, size).map(contributions => jsonOk(Json.toJson(contributions)))
  }
  
  // TODO should this be in the landing page controller?
  def getRightNowStats() = Action.async { implicit request =>
    
    val fAnnotations = annotations.countTotal()
    val fEditsToday = contributions.countToday()
    val fUsers = users.countUsers()
    
    val f = for {
      annotations <- fAnnotations
      editsToday <- fEditsToday
      users <- fUsers
    } yield (annotations, editsToday, users)
    
    f.map { case (annotations, editsToday, users) =>
      jsonOk(Json.obj(
        "annotations" -> annotations,
        "edits_today" -> editsToday,
        "users" -> users
      ))
    }
  }

  // TODO does this mix concerns too much?
  def getDashboardStats() = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    
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

package controllers.api

import controllers.{ BaseController, HasPrettyPrintJSON }
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import storage.DB
import models.contribution.ContributionService

class ContributionAPIController @Inject() (implicit val cache: CacheApi, val db: DB, context: ExecutionContext) extends BaseController with HasPrettyPrintJSON {
  
  def getContributionHistory(documentId: String, offset: Int, size: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    ContributionService.getHistory(documentId, offset, size).map(contributions => jsonOk(Json.toJson(contributions)))
  }
  
  /** Currently only available to admins **/
  def getGlobalStats() = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    ContributionService.getGlobalStats().map(stats => jsonOk(Json.toJson(stats)))
  }
 
}
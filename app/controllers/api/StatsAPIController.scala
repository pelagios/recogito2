package controllers.api

import controllers.{ BaseController, HasPrettyPrintJSON }
import javax.inject.Inject
import models.user.Roles._
import play.api.cache.CacheApi
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import storage.DB
import models.contribution.ContributionService

class StatsAPIController @Inject() (implicit val cache: CacheApi, val db: DB, context: ExecutionContext) extends BaseController with HasPrettyPrintJSON {
  
  /** Global stats - currently only available to admins **/
  def getGlobalStats() = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    ContributionService.getGlobalStats().map(stats => jsonOk(Json.toJson(stats)))
  }
 
}
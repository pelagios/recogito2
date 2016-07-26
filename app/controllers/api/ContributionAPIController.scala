package controllers.api

import controllers.{ BaseAuthController, ControllerContext, HasPrettyPrintJSON }
import javax.inject.Inject
import models.user.Roles._
import play.api.libs.json.Json
import models.contribution.ContributionService

class ContributionAPIController @Inject() (implicit val ctx: ControllerContext) extends BaseAuthController with HasPrettyPrintJSON {
  
  def getContributionHistory(documentId: String, offset: Int, size: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    ContributionService.getHistory(documentId, offset, size).map(contributions => jsonOk(Json.toJson(contributions)))
  }
  
  /** Currently only available to admins **/
  def getGlobalStats() = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    ContributionService.getGlobalStats().map(stats => jsonOk(Json.toJson(stats)))
  }
 
}
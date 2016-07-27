package controllers.api

import controllers.{ BaseAuthController, HasPrettyPrintJSON }
import javax.inject.Inject
import models.contribution.ContributionService
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import play.api.Configuration
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext

class ContributionAPIController @Inject() (
    val config: Configuration,
    val contributions: ContributionService,
    val documents: DocumentService,
    val users: UserService,
    implicit val ctx: ExecutionContext
  ) extends BaseAuthController(config, documents, users) with HasPrettyPrintJSON {
  
  def getContributionHistory(documentId: String, offset: Int, size: Int) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    contributions.getHistory(documentId, offset, size).map(contributions => jsonOk(Json.toJson(contributions)))
  }
  
  /** Currently only available to admins **/
  def getGlobalStats() = AsyncStack(AuthorityKey -> Admin) { implicit request =>
    contributions.getGlobalStats().map(stats => jsonOk(Json.toJson(stats)))
  }
 
}
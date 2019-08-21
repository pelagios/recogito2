package controllers.api.contribution

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseOptAuthController, Security, HasPrettyPrintJSON}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents 
import scala.concurrent.{ExecutionContext, Future}
import services.contribution.ContributionService
import services.document.DocumentService
import services.user.UserService

@Singleton
class ContributionAPIController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val contributions: ContributionService,
  val documents: DocumentService,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext
) extends BaseOptAuthController(components, config, documents, users) with HasPrettyPrintJSON {

  def getDocumentStats(id: String) = silhouette.UserAwareAction.async { implicit request => 
    documentResponse(id, request.identity, { case (doc, accesslevel) => 
      if (accesslevel.canReadData) {
        contributions.getDocumentStats(id).map { stats => 
          jsonOk(Json.toJson(stats))
        }
      } else {
        Future.successful(Forbidden)
      }
    })
  }
  
}

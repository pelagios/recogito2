package controllers.api.network

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseOptAuthController, Security, HasPrettyPrintJSON}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.{ExecutionContext, Future}
import services.document.DocumentService
import services.user.UserService

@Singleton
class NetworkAPIController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val documents: DocumentService,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext
) extends BaseOptAuthController(components, config, documents, users) with HasPrettyPrintJSON {

  def getNetwork(docId: String) = silhouette.UserAwareAction.async { implicit request => 
    documentResponse(docId, request.identity, { case (doc, accesslevel) => 
      if (accesslevel.canReadData) {

        // TODO
        documents.getNetwork(docId).map { result => Ok(result.toString)}
      
      } else {
        Future.successful(Forbidden)
      }
    })
  }
  
}

package controllers.my.directory.search

import controllers.{Security, HasPrettyPrintJSON}
import controllers.my.directory.ConfiguredPresentation
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request, AbstractController, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import services.document.DocumentService

@Singleton
class SearchController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[Security.Env],
  documentService: DocumentService,
  implicit val ctx: ExecutionContext
) extends AbstractController(components) 
    with HasPrettyPrintJSON {

  import ConfiguredPresentation._

  private def getSearchOpts(request: Request[AnyContent]): Option[SearchOptions] = 
    request.body.asJson match {
      case Some(json) => 
        Try(Json.fromJson[SearchOptions](json).get).toOption

      case None => // In case of no JSON payload, try URL params
        val params = request.queryString
          .mapValues(_.headOption)
          .filter(_._2.isDefined)
          .mapValues(_.get)

        // TODO other params

        params.get("q").map { q => // Require at least a query phrase
          SearchOptions(
            Some(q),
            params.get("in").flatMap(Scope.withName).getOrElse(Scope.ALL_OF_RECOGITO),
            None, None, None
          ) 
        }

    }
  
  /** Search all of public Recogito, plus my own accessible documents **/
  def search = silhouette.UserAwareAction.async { implicit request =>
    getSearchOpts(request) match {
      case Some(opts) => 
        documentService.searchAll(request.identity.map(_.username), opts.query.get).map { documents => 
          val presentation = ConfiguredPresentation.forMyDocument(documents, None, None)
          jsonOk(Json.toJson(presentation))
        }

      case None =>
        Future.successful(BadRequest)
    } 
  }

}

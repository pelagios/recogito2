package controllers.my.directory.search

import controllers.{Security, HasPrettyPrintJSON}
import controllers.my.directory.ConfiguredPresentation
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContent, Request, AbstractController, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import services.HasDate
import services.document.DocumentService
import services.document.search._

@Singleton
class SearchController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[Security.Env],
  documentService: DocumentService,
  implicit val ctx: ExecutionContext
) extends AbstractController(components) 
    with HasPrettyPrintJSON
    with HasDate {

  import ConfiguredPresentation._

  implicit val searchOptionsReads: Reads[SearchArgs] = (
    (JsPath \ "q").readNullable[String] and
    (JsPath \ "in").readNullable[String]
      .map(_.flatMap(Scope.withName).getOrElse(Scope.ALL)) and
    (JsPath \ "type").readNullable[String]
      .map(_.flatMap(DocumentType.withName)) and
    (JsPath \ "owner").readNullable[String] and
    (JsPath \ "max_age").readNullable[DateTime]
  )(SearchArgs.apply _) 

  private def parseQueryString(params: Map[String, String]) = 
    params.get("q").map { q => // Require at least a query phrase
      SearchArgs(
        Some(q),
        params.get("in").flatMap(Scope.withName).getOrElse(Scope.ALL),
        params.get("type").flatMap(DocumentType.withName),
        params.get("owner"),
        params.get("max_age").flatMap(parseDate)
      ) 
    }

  def parseSearchArgs(request: Request[AnyContent]) = request.body.asJson match {
    case Some(json) =>
      Try(Json.fromJson[SearchArgs](json).get).toOption

    case None => // Try query string instead
      val asMap = request.queryString
        .mapValues(_.headOption)
        .filter(_._2.isDefined)
        .mapValues(_.get)
      parseQueryString(asMap)
  }
  
  /** Search all of public Recogito, plus my own accessible documents **/
  def search = silhouette.UserAwareAction.async { implicit request =>
    parseSearchArgs(request) match {
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

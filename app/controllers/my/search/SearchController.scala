package controllers.my.search

import controllers.{Security, HasPrettyPrintJSON}
import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request, AbstractController, ControllerComponents}
import scala.concurrent.ExecutionContext
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

  private def getSearchOpts(request: Request[AnyContent]) = {
    request.body.asJson.flatMap(json => 
      Try(Json.fromJson[SearchOptions](json).get).toOption)
  }
  
  /** Search all of public Recogito, plus my own accessible documents **/
  def searchAll(query: String) = silhouette.UserAwareAction.async { implicit request =>
    documentService.searchAll(request.identity.map(_.username), query).map { documents => 
      jsonOk(Json.toJson(documents.map { doc => 
        Json.obj("id" -> doc.getId, "title" -> doc.getTitle)
      }))
    }
  }

  /** Searches just my own documents **/
  def searchMy(query: String) = silhouette.SecuredAction.async { implicit request => 
    documentService.searchMyDocuments(request.identity.username, query).map { documents => 
      jsonOk(Json.toJson(documents.map { doc => 
        Json.obj("id" -> doc.getId, "title" -> doc.getTitle)
      }))
    }
  }

  /** Searches just the documents shared with me **/
  def searchSharedWithMe(query: String) = silhouette.SecuredAction.async { implicit request =>
    documentService.searchSharedWithMe(request.identity.username, query).map { documents => 
      jsonOk(Json.toJson(documents.map { doc => 
        Json.obj("id" -> doc.getId, "title" -> doc.getTitle)
      }))
    }
  }

}

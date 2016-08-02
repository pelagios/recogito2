package controllers.document.downloads

import akka.util.ByteString
import akka.stream.scaladsl.Source
import controllers.{ BaseAuthController, WebJarAssets }
import controllers.document.downloads.serializers._
import javax.inject.Inject
import models.annotation.AnnotationService
import models.document.DocumentService
import models.user.UserService
import models.user.Roles._
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.iteratee.Enumerator
import play.api.http.HttpEntity
import play.api.libs.streams.Streams
import scala.concurrent.ExecutionContext

class DownloadsController @Inject() (
    val config: Configuration,
    val annotations: AnnotationService,
    val documents: DocumentService,
    val users: UserService,
    implicit val webjars: WebJarAssets,
    implicit val ctx: ExecutionContext
  ) extends BaseAuthController(config, documents, users) with CSVSerializer {

  /** TODO this view should be available without login, if the document is set to public **/
  def showDownloadOptions(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    documentResponse(documentId, loggedIn.user.getUsername,
        { case (document, fileparts, accesslevel) =>  Ok(views.html.document.downloads.index(Some(loggedIn.user.getUsername), document, accesslevel)) })
  }

  def downloadAnnotations(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    annotations.findByDocId(documentId).map { annotations =>
      val enumerator = Enumerator.enumerate(annotations.map(t => Json.stringify(Json.toJson(t._1)) + "\n"))
      val source = Source.fromPublisher(Streams.enumeratorToPublisher(enumerator)).map(ByteString.apply)
      Ok.sendEntity(HttpEntity.Streamed(source, None, Some("app/json")))
    }
  }
  
  def downloadCSV(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    annotations.findByDocId(documentId).map { annotations =>
      Ok(toCSV(annotations.map(_._1))).withHeaders(
        CONTENT_DISPOSITION -> { "attachment; filename=" + documentId + ".csv" },
        CONTENT_TYPE -> "text/csv"
      )
    }
  }

}

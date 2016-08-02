package controllers.document.downloads

import akka.util.ByteString
import akka.stream.scaladsl.Source
import controllers.{ BaseOptAuthController, WebJarAssets }
import controllers.document.downloads.serializers._
import javax.inject.Inject
import models.annotation.AnnotationService
import models.document.DocumentService
import models.user.UserService
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.iteratee.Enumerator
import play.api.http.HttpEntity
import play.api.libs.streams.Streams
import scala.concurrent.{ ExecutionContext, Future }

class DownloadsController @Inject() (
    val config: Configuration,
    val annotations: AnnotationService,
    val documents: DocumentService,
    val users: UserService,
    implicit val webjars: WebJarAssets,
    implicit val ctx: ExecutionContext
  ) extends BaseOptAuthController(config, documents, users) with CSVSerializer {

  def showDownloadOptions(documentId: String) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user.getUsername)
    documentReadResponse(documentId, maybeUser, { case (document, fileparts, accesslevel) =>
      Future.successful(Ok(views.html.document.downloads.index(maybeUser, document, accesslevel)))
    })
  }

  def downloadAnnotations(documentId: String) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user.getUsername)
    documentReadResponse(documentId, maybeUser, { case (document, fileparts, accesslevel) =>
      annotations.findByDocId(documentId).map { annotations =>
        val enumerator = Enumerator.enumerate(annotations.map(t => Json.stringify(Json.toJson(t._1)) + "\n"))
        val source = Source.fromPublisher(Streams.enumeratorToPublisher(enumerator)).map(ByteString.apply)
        Ok.sendEntity(HttpEntity.Streamed(source, None, Some("app/json")))
      }
    })
  }
  
  def downloadCSV(documentId: String) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user.getUsername)
    documentReadResponse(documentId, maybeUser, { case (document, fileparts, accesslevel) =>
      annotations.findByDocId(documentId).map { annotations =>
        Ok(toCSV(annotations.map(_._1)))
          .withHeaders(CONTENT_DISPOSITION -> { "attachment; filename=" + documentId + ".csv" })
      }
    })
  }

}

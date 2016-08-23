package controllers.document.downloads

import akka.util.ByteString
import akka.stream.scaladsl.Source
import controllers.{ BaseOptAuthController, WebJarAssets }
import controllers.document.downloads.serializers._
import javax.inject.Inject
import models.annotation.AnnotationService
import models.document.DocumentService
import models.place.PlaceService
import models.user.UserService
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.iteratee.Enumerator
import play.api.http.HttpEntity
import play.api.libs.streams.Streams
import scala.concurrent.ExecutionContext

class DownloadsController @Inject() (
    val config: Configuration,
    val users: UserService,
    implicit val annotations: AnnotationService,
    implicit val documents: DocumentService,
    implicit val places: PlaceService,
    implicit val webjars: WebJarAssets,
    implicit val ctx: ExecutionContext
  ) extends BaseOptAuthController(config, documents, users)
      with CSVSerializer
      with GeoJSONSerializer {

  def showDownloadOptions(documentId: String) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user)
    documentReadResponse(documentId, maybeUser, { case (doc, accesslevel) =>
      annotations.countByDocId(documentId).map { documentAnnotationCount =>
        Ok(views.html.document.downloads.index(doc, maybeUser, accesslevel, documentAnnotationCount))
      }
    })
  }

  def downloadAnnotations(documentId: String) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user)
    documentReadResponse(documentId, maybeUser, { case (_, _) => // Used just for the access permission check
      annotations.findByDocId(documentId).map { annotations =>
        val enumerator = Enumerator.enumerate(annotations.map(t => Json.stringify(Json.toJson(t._1)) + "\n"))
        val source = Source.fromPublisher(Streams.enumeratorToPublisher(enumerator)).map(ByteString.apply)
        Ok.sendEntity(HttpEntity.Streamed(source, None, Some("app/json")))
      }
    })
  }

  def downloadCSV(documentId: String) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user)
    documentReadResponse(documentId, maybeUser, { case (_, _) => // Used just for the access permission check
      annotationsToCSV(documentId).map { csv =>
        Ok(csv).withHeaders(CONTENT_DISPOSITION -> { "attachment; filename=" + documentId + ".csv" })
      }
    })
  }

  def downloadGeoJSON(documentId: String) = AsyncStack { implicit request =>
    val maybeUser = loggedIn.map(_.user)
    documentReadResponse(documentId, maybeUser, { case (_, _) => // Used just for the access permission check
      placesToGeoJSON(documentId).map { featureCollection =>
        Ok(Json.prettyPrint(Json.toJson(featureCollection)))
          .withHeaders(CONTENT_DISPOSITION -> { "attachment; filename=" + documentId + ".json" })
      }
    })
  }

}

package controllers.document.downloads.writers

import controllers.BaseAuthController
import javax.inject.Inject
import models.annotation.{ Annotation, AnnotationService }
import models.user.Roles._
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import storage.DB
import play.api.libs.json.Json
import play.api.libs.iteratee.Enumerator
import play.api.mvc.{ Result, ResponseHeader }
import play.api.http.HttpEntity
import akka.stream.scaladsl.StreamConverters
import akka.stream.scaladsl.Source
import play.api.libs.streams.Streams
import akka.util.ByteString

class JSONLinesWriterController @Inject() (implicit val cache: CacheApi, val db: DB) extends BaseAuthController {
  
  private val NEWLINE = "\n"
  
  def downloadAnnotations(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    AnnotationService.findByDocId(documentId, Int.MaxValue).map { annotations =>
      val enumerator = Enumerator.enumerate(annotations.map(t => Json.stringify(Json.toJson(t._1)) + NEWLINE))
      val source = Source.fromPublisher(Streams.enumeratorToPublisher(enumerator)).map(ByteString.apply)
      Ok.sendEntity(HttpEntity.Streamed(source, None, Some("app/json")))
    }
  }
  
}
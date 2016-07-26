package controllers.document.downloads.writers

import akka.util.ByteString
import akka.stream.scaladsl.Source
import controllers.{ BaseAuthController, ControllerContext }
import javax.inject.Inject
import models.annotation.AnnotationService
import models.user.Roles._
import play.api.libs.json.Json
import play.api.libs.iteratee.Enumerator
import play.api.http.HttpEntity
import play.api.libs.streams.Streams

class JSONLinesWriterController @Inject() (implicit val ctx: ControllerContext) extends BaseAuthController {
  
  private val NEWLINE = "\n"
  
  def downloadAnnotations(documentId: String) = AsyncStack(AuthorityKey -> Normal) { implicit request =>
    AnnotationService.findByDocId(documentId).map { annotations =>
      val enumerator = Enumerator.enumerate(annotations.map(t => Json.stringify(Json.toJson(t._1)) + NEWLINE))
      val source = Source.fromPublisher(Streams.enumeratorToPublisher(enumerator)).map(ByteString.apply)
      Ok.sendEntity(HttpEntity.Streamed(source, None, Some("app/json")))
    }
  }
  
}
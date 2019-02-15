package controllers.document.downloads.serializers.document.spacy

import play.api.Configuration
import play.api.libs.json._
import play.api.libs.Files.TemporaryFileCreator
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.AnnotationService
import services.document.DocumentInfo
import storage.uploads.Uploads

trait TEIToSpacy {

  def teiToSpacy(
    doc: DocumentInfo
  )(implicit 
    annotationService: AnnotationService,
    conf: Configuration,
    ctx: ExecutionContext,
    tmpFile: TemporaryFileCreator,
    uploads: Uploads
  ): Future[JsValue] = ???

}
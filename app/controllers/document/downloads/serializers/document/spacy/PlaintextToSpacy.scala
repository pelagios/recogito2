package controllers.document.downloads.serializers.document.spacy

import java.io.File
import play.api.Configuration
import play.api.libs.Files.TemporaryFileCreator
import scala.concurrent.{ExecutionContext, Future}
import services.document.DocumentInfo
import services.annotation.AnnotationService
import storage.uploads.Uploads

trait PlaintextToSpacy {

  def plaintextToSpacy(
    doc: DocumentInfo
  )(implicit 
    annotationService: AnnotationService,
    conf: Configuration,
    ctx: ExecutionContext,
    tmpFile: TemporaryFileCreator,
    uploads: Uploads
  ): Future[File] = ???

}
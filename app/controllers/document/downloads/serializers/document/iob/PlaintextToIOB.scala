package controllers.document.downloads.serializers.document.iob

import java.io.{File, PrintWriter}
import java.nio.file.Paths
import java.util.UUID
import play.api.Configuration
import play.api.libs.Files.TemporaryFileCreator
import services.annotation.AnnotationService
import services.document.DocumentInfo
import scala.concurrent.{ExecutionContext, Future}
import storage.TempDir
import storage.uploads.Uploads

trait PlaintextToIOB {

  def plaintextToIOB(
    doc: DocumentInfo
  )(implicit 
    annotationService: AnnotationService,
    conf: Configuration,
    ctx: ExecutionContext,
    tmpFile: TemporaryFileCreator,
    uploads: Uploads
  ): Future[File] = {
    val tmp = tmpFile.create(Paths.get(TempDir.get(), s"${UUID.randomUUID}.iob.txt"))
    val underlying = tmp.path.toFile
    val writer = new PrintWriter(underlying)

    val fTokens = 
      uploads
        .readTextfile(doc.owner.getUsername, doc.id, doc.fileparts.head.getFile)
        .map(_.map(_.split(" ").toSeq).getOrElse(Seq.empty[String]))

    fTokens.map { tokens => 
      tokens.foreach(token => writer.println(token))
      underlying
    }
  }

}
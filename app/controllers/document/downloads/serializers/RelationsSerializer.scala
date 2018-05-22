package controllers.document.downloads.serializers

import java.nio.file.Paths
import java.util.UUID
import kantan.csv.CsvConfiguration
import kantan.csv.CsvConfiguration.{Header, QuotePolicy}
import kantan.csv.ops._
import play.api.Configuration
import play.api.libs.Files.TemporaryFileCreator
import scala.concurrent.ExecutionContext
import services.annotation.AnnotationService
import storage.TempDir
import storage.es.ES

trait RelationsSerializer {
  
  /** Exports as Gephi Adjacency list **/
  def relationsToGephiCSV(documentId: String)(
    implicit annotationService: AnnotationService, tmpFile: TemporaryFileCreator, conf: Configuration, ctx: ExecutionContext
  ) = {
    val fAnnotations = annotationService.findWithRelationByDocId(documentId, 0, ES.MAX_SIZE)
    fAnnotations.map { annotations =>
      val tmp = tmpFile.create(Paths.get(TempDir.get(), s"${UUID.randomUUID}.csv"))
      val underlying = tmp.path.toFile
      val config = CsvConfiguration(',', '"', QuotePolicy.Always, Header.None)
      
      val writer = underlying.asCsvWriter[Seq[String]](config)
      
      annotations.foreach { annotation =>
        val destinations = annotation.relations.map(_.relatesTo)
        val row = annotation.annotationId +: destinations
        writer.write(row.map(_.toString))
      }
      
      writer.close()      
      underlying
    }
  }
  
}
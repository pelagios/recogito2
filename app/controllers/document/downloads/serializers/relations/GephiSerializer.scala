package controllers.document.downloads.serializers.relations

import controllers.document.downloads.serializers.BaseSerializer
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

trait GephiSerializer extends BaseSerializer {
  
  def relationsToGephiNodes(documentId: String)(
    implicit annotationService: AnnotationService, tmpFile: TemporaryFileCreator, conf: Configuration, ctx: ExecutionContext
  ) = {
    annotationService.findWithRelationByDocId(documentId, 0, ES.MAX_SIZE).map { annotations =>
      val header = Seq("Id", "Label")

      val tmp = tmpFile.create(Paths.get(TempDir.get(), s"${UUID.randomUUID}.csv"))
      val underlying = tmp.path.toFile
      val config = CsvConfiguration(',', '"', QuotePolicy.Always, Header.Explicit(header))
      
      val writer = underlying.asCsvWriter[Seq[String]](config)
      
      annotations.foreach { annotation =>
        val row = Seq(annotation.annotationId.toString, getFirstQuote(annotation).getOrElse(""))
        writer.write(row)
      }
      
      writer.close()      
      underlying
    } 
  }
  
  def relationsToGephiEdges(documentId: String)(
    implicit annotationService: AnnotationService, tmpFile: TemporaryFileCreator, conf: Configuration, ctx: ExecutionContext
  ) = {
    annotationService.findWithRelationByDocId(documentId, 0, ES.MAX_SIZE).map { annotations =>
      val header = Seq("Source", "Target", "Label")
      
      val tmp = tmpFile.create(Paths.get(TempDir.get(), s"${UUID.randomUUID}.csv"))
      val underlying = tmp.path.toFile
      val config = CsvConfiguration(',', '"', QuotePolicy.Always, Header.Explicit(header))
      
      val writer = underlying.asCsvWriter[Seq[String]](config)
      
      annotations.foreach { annotation =>
        annotation.relations.foreach { relation =>
          val tags = relation.bodies.map(_.value)
          val row = Seq(annotation.annotationId.toString, relation.relatesTo.toString, tags.mkString)
          writer.write(row)
        }
      }
      
      writer.close()      
      underlying
    }
  }
  
}
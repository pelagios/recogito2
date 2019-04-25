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

trait RelationsToTriplesCSV extends BaseSerializer {
  
  def relationsToTriplesCSV(
    documentId: String
  )(implicit 
      annotationService: AnnotationService,
      tmpFile: TemporaryFileCreator,
      conf: Configuration,
      ctx: ExecutionContext
  ) = {
    annotationService.findWithRelationByDocId(documentId, 0, ES.MAX_SIZE).map { annotations => 
      val header = Seq("from_quote", "relation", "to_quote")

      val tmp = tmpFile.create(Paths.get(TempDir.get(), s"${UUID.randomUUID}.csv"))
      val underlying = tmp.path.toFile
      val config = CsvConfiguration(',', '"', QuotePolicy.Always, Header.Explicit(header))
      
      val writer = underlying.asCsvWriter[Seq[String]](config)

      annotations.foreach { annotation =>
        val fromQuote = getFirstQuoteOrTranscription(annotation)

        annotation.relations.foreach { relation => 
          val toAnnotation = annotations.find(_.annotationId == relation.relatesTo).get
          val toQuote = getFirstQuoteOrTranscription(toAnnotation)
        
          relation.bodies.foreach { relationBody => 
            val row = Seq(fromQuote.get, relationBody.value, toQuote.get)
            writer.write(row)
          }
        }
      }

      writer.close()      
      underlying
    }
  }
  
}
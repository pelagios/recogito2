package controllers.document.downloads.serializers.document.csv

import controllers.HasCSVParsing
import controllers.document.downloads.serializers.BaseSerializer
import java.io.{BufferedInputStream, File, FileInputStream}
import java.nio.file.Paths
import java.util.UUID
import java.util.zip.{ZipEntry, ZipOutputStream}
import kantan.csv.CsvConfiguration
import kantan.csv.CsvConfiguration.{Header, QuotePolicy}
import kantan.csv.ops._
import kantan.csv.engine.commons._
import play.api.Configuration
import play.api.libs.Files.TemporaryFileCreator
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import services.ContentType
import services.annotation.{AnnotationService, AnnotationBody}
import services.document.ExtendedDocumentMetadata
import services.entity.Entity
import services.entity.builtin.EntityService
import services.generated.tables.records.DocumentFilepartRecord
import storage.TempDir
import storage.uploads.Uploads

trait DatatableToCSV extends BaseSerializer with HasCSVParsing {
  
  private def findPlace(body: AnnotationBody, places: Seq[Entity]): Option[Entity] =
    body.uri.flatMap { uri =>
      places.find(_.uris.contains(uri))
    }
  
  /** Exports a ZIP of user-uploaded tables, with annotation info merged into the original CSVs **/ 
  def exportMergedTables(
    doc: ExtendedDocumentMetadata
  )(implicit
      annotationService: AnnotationService, 
      entityService: EntityService,
      uploads: Uploads,
      tmpFile: TemporaryFileCreator,
      conf: Configuration,
      ctx: ExecutionContext
  ): Future[(File, String)] = {
    
    def createZip(parts: Seq[(DocumentFilepartRecord, File)]): File = {
      // val tmp = tmpFile.create(TMP_DIR, UUID.randomUUID + ".zip")
      val p = Paths.get(TempDir.get(), s"${UUID.randomUUID}.zip")
      val tmp = tmpFile.create(p)
      val zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(p))
      
      parts.foreach { case (part, file) =>
        val filename = 
          if (part.getTitle.endsWith(".csv")) part.getTitle
          else part.getTitle + ".csv"
          
        zip.putNextEntry(new ZipEntry(filename))
        val in = new BufferedInputStream(new FileInputStream(file))
        
        var b = in.read()
        while (b > -1) {
          zip.write(b)
          b = in.read()
        }
        
        in.close()
        zip.closeEntry()
      }
      
      zip.close()
      p.toFile
    }
    
    exportMergedDocument[(File, String)](doc, { case (annotations, places, documentDir) =>
      
      def extendRow(row: List[String], part: DocumentFilepartRecord, index: Int): List[String] = {
        val anchor = "row:" + index
        val maybeAnnotation = annotations.find { a => 
          a.annotates.filepartId == part.getId && a.anchor == anchor
        }
        
        val maybeFirstEntity = maybeAnnotation.flatMap(getFirstEntityBody(_))
        val maybePlace = maybeFirstEntity.flatMap(body => findPlace(body, places))
        
        val tags = maybeAnnotation.map(a => getTagBodies(a).flatMap(_.value))
          .getOrElse(Seq.empty[String])

        val comments = maybeAnnotation.map(a => getCommentBodies(a).flatMap(_.value))
          .getOrElse(Seq.empty[String])

        row ++ Seq(
          maybeFirstEntity.map(_.hasType.toString).getOrElse(""),
          maybeFirstEntity.flatMap(_.uri).getOrElse(""),
          maybePlace.flatMap(_.representativePoint.map(_.y.toString)).getOrElse(""),
          maybePlace.flatMap(_.representativePoint.map(_.x.toString)).getOrElse(""),
          tags.mkString("|"),
          comments.mkString("|")
        )
      }
            
      val tables =
        doc.fileparts
          .withFilter(part => ContentType.withName(part.getContentType).map(_.isData).getOrElse(false))
          .map(part => (part, new File(documentDir, part.getFile)))
          
      val outputFiles = tables.map { case (part, file) =>   
        val header = Source.fromFile(file).getLines.next
        val delimiter = guessDelimiter(header)       
        val headerConfig = CsvConfiguration(delimiter, '"', QuotePolicy.WhenNeeded, Header.None)
        val headerFields = 
          header.asCsvReader[Seq[String]](headerConfig).toIterator.next.get ++
          Seq( // Additional columns added by Recogito
            "recogito_type",
            "recogito_uri",
            "recogito_lat",
            "recogito_lon",
            "recogito_tags",
            "recogito_comments")
     
        val p = Paths.get(TempDir.get(), s"${UUID.randomUUID}.csv")
        val tmp = tmpFile.create(p)
        val underlying = p.toFile
        val writerConfig = CsvConfiguration(',', '"', QuotePolicy.Always, Header.Explicit(headerFields))
        val writer = underlying.asCsvWriter[Seq[String]](writerConfig)
        
        parseCSV(file, delimiter, header = true, { case (row, idx) =>
          extendRow(row, part, idx)
        }).foreach { _ match {
          case Some(row) => writer.write(row)
          case None => writer.write(Seq.empty[String])
        }}
        
        writer.close()
        
        (part, underlying)
      }
      
      if (outputFiles.isEmpty)
        // Can't ever happen from the UI
        throw new RuntimeException("Attempt to export merged table from a non-table document")
      else if (outputFiles.size == 1)
        // Single table - export CSV directly
        (outputFiles.head._2, doc.id + ".csv")
      else
        // Multiple tables - package into a Zip
        (createZip(outputFiles), doc.id + ".zip")
    })
  }  
  
}
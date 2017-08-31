package controllers.document.downloads.serializers

import controllers.HasCSVParsing
import java.io.{ BufferedInputStream, File, FileInputStream, FileOutputStream }
import java.util.UUID
import java.util.zip.{ ZipEntry, ZipOutputStream }
import kantan.csv.CsvConfiguration
import kantan.csv.CsvConfiguration.{ Header, QuotePolicy }
import kantan.csv.ops._
import models.ContentType
import models.annotation.{ Annotation, AnnotationBody, AnnotationService }
import models.document.DocumentInfo
import models.place.{ Place, PlaceService }
import models.generated.tables.records.DocumentFilepartRecord
import play.api.libs.Files.TemporaryFile
import scala.concurrent.{ Future, ExecutionContext }
import scala.io.Source
import storage.Uploads

trait CSVSerializer extends BaseSerializer with HasCSVParsing {

  private val EMPTY     = ""
  
  private def findPlace(body: AnnotationBody, places: Seq[Place]): Option[Place] =
    body.uri.flatMap { uri =>
      places.find(_.uris.contains(uri))
    }

  /** Exports the annotations for the document to the standard Recogito CSV output format **/ 
  def annotationsToCSV(documentId: String)(implicit annotationService: AnnotationService, placeService: PlaceService, ctx: ExecutionContext) = {

    def serializeOne(a: Annotation, places: Seq[Place]): Seq[String] = {
      val firstEntity = getFirstEntityBody(a)
      val maybePlace = firstEntity.flatMap(body => findPlace(body, places))
      
      val quoteOrTranscription =
        if (a.annotates.contentType.isText)
          getFirstQuote(a)
        else if (a.annotates.contentType.isImage)
          getFirstTranscription(a)
        else None
        
      val placeTypes = maybePlace.map(_.placeTypes.map(_._1).mkString(","))

      Seq(a.annotationId.toString,
          quoteOrTranscription.getOrElse(EMPTY),
          a.anchor,
          firstEntity.map(_.hasType.toString).getOrElse(EMPTY),
          firstEntity.flatMap(_.uri).getOrElse(EMPTY),
          maybePlace.map(_.titles.mkString("|")).getOrElse(EMPTY),
          maybePlace.flatMap(_.representativePoint.map(_.y.toString)).getOrElse(EMPTY),
          maybePlace.flatMap(_.representativePoint.map(_.x.toString)).getOrElse(EMPTY),
          maybePlace.map(_.placeTypes.map(_._1).mkString(",")).getOrElse(EMPTY),
          firstEntity.flatMap(_.status.map(_.value.toString)).getOrElse(EMPTY),
          getTagBodies(a).flatMap(_.value).mkString("|"),
          getCommentBodies(a).flatMap(_.value).mkString("|"))
    }

    val annotationQuery = annotationService.findByDocId(documentId)
    val placeQuery = placeService.listPlacesInDocument(documentId)

    val f = for {
      annotations <- annotationQuery
      places <- placeQuery
    } yield (annotations, places.items.map(_._1))

    f.map { case (annotations, places) =>
      scala.concurrent.blocking {
        val header = Seq("UUID", "QUOTE_TRANSCRIPTION", "ANCHOR", "TYPE", "URI", "VOCAB_LABEL", "LAT", "LNG", "PLACE_TYPE", "VERIFICATION_STATUS", "TAGS", "COMMENTS")
        
        val tmp = new TemporaryFile(new File(TMP_DIR, UUID.randomUUID + ".csv"))
        val config = CsvConfiguration(',', '"', QuotePolicy.Always, Header.Explicit(header))
        val writer = tmp.file.asCsvWriter[Seq[String]](config)
        
        val tupled = sort(annotations.map(_._1)).map(a => serializeOne(a, places))
        tupled.foreach(t => writer.write(t))
        writer.close()
        
        tmp.file
      }
    }
  }
  
  /** Exports a ZIP of user-uploaded tables, with annotation info merged into the original CSVs **/ 
  def exportMergedTables(
    doc: DocumentInfo
  )(
    implicit annotationService: AnnotationService, 
      placeService: PlaceService,
      uploads: Uploads,
      ctx: ExecutionContext): Future[(File, String)] = {
    
    def createZip(parts: Seq[(DocumentFilepartRecord, File)]): File = {
      val tmp = new TemporaryFile(new File(TMP_DIR, UUID.randomUUID + ".zip"))
      val zip = new ZipOutputStream(new FileOutputStream(tmp.file))
      
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
      tmp.file
    }
    
    exportMergedDocument[(File, String)](doc, { case (annotations, places, documentDir) =>
      
      def extendRow(row: List[String], index: Int): List[String] = {
        val anchor = "row:" + index
        val maybeAnnotation = annotations.find(_.anchor == anchor)
        
        val maybeFirstEntity = maybeAnnotation.flatMap(getFirstEntityBody(_))
        val maybePlace = maybeFirstEntity.flatMap(body => findPlace(body, places))
        
        row ++ Seq(
          maybeFirstEntity.map(_.hasType.toString).getOrElse(EMPTY),
          maybeFirstEntity.flatMap(_.uri).getOrElse(EMPTY),
          maybePlace.flatMap(_.representativePoint.map(_.y.toString)).getOrElse(EMPTY),
          maybePlace.flatMap(_.representativePoint.map(_.x.toString)).getOrElse(EMPTY)
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
            "recogito_lon")
     
        val tmp = new TemporaryFile(new File(TMP_DIR, UUID.randomUUID + ".csv"))
        val writerConfig = CsvConfiguration(',', '"', QuotePolicy.Always, Header.Explicit(headerFields))
        val writer = tmp.file.asCsvWriter[Seq[String]](writerConfig)
        
        parseCSV(file, delimiter, header = true, { case (row, idx) =>
          extendRow(row, idx)
        }).foreach { _ match {
          case Some(row) => writer.write(row)
          case None => writer.write(Seq.empty[String])
        }}
        
        writer.close()
        
        (part, tmp.file)
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

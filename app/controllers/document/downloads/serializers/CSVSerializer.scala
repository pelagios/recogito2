package controllers.document.downloads.serializers

import java.io.File
import java.util.UUID
import kantan.csv.ops._
import kantan.codecs.Result.Success
import models.annotation.{ Annotation, AnnotationBody, AnnotationService }
import models.document.DocumentInfo
import models.place.{ Place, PlaceService }
import play.api.libs.Files.TemporaryFile
import scala.concurrent.{ Future, ExecutionContext }
import storage.Uploads
import models.ContentType
import scala.io.Source

trait CSVSerializer extends BaseSerializer {

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
        val writer = tmp.file.asCsvWriter[Seq[String]](';', header)
        
        val tupled = sort(annotations.map(_._1)).map(a => serializeOne(a, places))
        tupled.foreach(t => writer.write(t))
        writer.close()
        
        tmp.file
      }
    }
  }
  
  /** Exports a ZIP of user-uploaded tables, with annotation info merged into the original CSVs **/ 
  def tablesToZIP(doc: DocumentInfo)(implicit annotationService: AnnotationService, placeService: PlaceService, uploads: Uploads, ctx: ExecutionContext) = {
    val fAnnotations = annotationService.findByDocId(doc.id)
    val fPlaces = placeService.listPlacesInDocument(doc.id)

    val f = for {
      annotations <- fAnnotations
      places <- fPlaces
    } yield (annotations.map(_._1), places.items.map(_._1))
    
    f.map { case (annotations, places) =>
      scala.concurrent.blocking {
           
        def guessDelimiter(line: String): Char = {
          // This test is pretty trivial but seems to be applied elsewhere (see e.g.
          // http://stackoverflow.com/questions/14693929/ruby-how-can-i-detect-intelligently-guess-the-delimiter-used-in-a-csv-file)
          // Simply count the most-used candidate
          val choices = Seq(',', ';', '\t', '|')
          val ranked = choices
            .map(char => (char, line.count(_ == char)))
            .sortBy(_._2).reverse
            
          ranked.head._1
        }
        
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
        
        val documentDir = uploads.getDocumentDir(doc.owner.getUsername, doc.id).get
        val tables =
          doc.fileparts
            .withFilter(part => ContentType.withName(part.getContentType).map(_.isData).getOrElse(false))
            .map(part => new File(documentDir, part.getFile))
            
        val outputFiles = tables.map { table =>   
          val header = Source.fromFile(table).getLines.next
          val delimiter = guessDelimiter(header)        
          val headerFields = 
            header.asCsvReader[Seq[String]](delimiter, header = false).toIterator.next.get ++
            // Additional columns added by Recogito
            Seq(
              "recogito_type",
              "recogito_uri",
              "recogito_lat",
              "recogito_lon")
       
          val tmp = new TemporaryFile(new File(TMP_DIR, UUID.randomUUID + ".csv"))
          val writer = tmp.file.asCsvWriter[Seq[String]](delimiter, headerFields)
          
          var rowCounter = 0
  
          val extendedRows = table.asCsvReader[List[String]](delimiter, header = true).map {
            case Success(row) =>
              val extendedRow = extendRow(row, rowCounter)
              rowCounter += 1
              extendedRow
              
            case _ => 
              rowCounter += 1
              Seq.empty[String]
          }
          
          extendedRows.foreach(row => writer.write(row))
          writer.close()
          
          tmp.file
        }
      
        outputFiles.head
      }
    }
  }

}

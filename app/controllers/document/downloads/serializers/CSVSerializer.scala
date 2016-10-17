package controllers.document.downloads.serializers

import java.io.File
import java.util.UUID
import kantan.csv.ops._
import models.annotation.{Annotation, AnnotationService}
import models.place.{Place, PlaceService}
import play.api.libs.Files.TemporaryFile
import scala.concurrent.ExecutionContext

trait CSVSerializer extends BaseSerializer {

  private val TMP_DIR = System.getProperty("java.io.tmpdir")

  private val EMPTY     = ""

  def annotationsToCSV(documentId: String)(implicit annotationService: AnnotationService, placeService: PlaceService, ctx: ExecutionContext) = {

    def serializeOne(a: Annotation, places: Seq[Place]): Seq[String] = {
      val firstEntity = getFirstEntityBody(a)
      val maybePlace = firstEntity.flatMap(_.uri.flatMap { uri =>
        places.find(_.uris.contains(uri))
      })
      val quoteOrTranscription =
        if (a.annotates.contentType.isText)
          getFirstQuote(a)
        else if (a.annotates.contentType.isImage)
          getFirstTranscription(a)
        else None

      Seq(a.annotationId.toString,
          quoteOrTranscription.getOrElse(EMPTY),
          a.anchor,
          firstEntity.map(_.hasType.toString).getOrElse(EMPTY),
          firstEntity.flatMap(_.uri).getOrElse(EMPTY),
          maybePlace.map(_.titles.mkString(",")).getOrElse(EMPTY),
          maybePlace.flatMap(_.representativePoint.map(_.y.toString)).getOrElse(EMPTY),
          maybePlace.flatMap(_.representativePoint.map(_.x.toString)).getOrElse(EMPTY),
          firstEntity.flatMap(_.status.map(_.value.toString)).getOrElse(EMPTY))
    }

    // This way, futures start immediately, in parallel
    val annotationQuery = annotationService.findByDocId(documentId)
    val placeQuery = placeService.listPlacesInDocument(documentId)

    // Fetch annotations
    val f = for {
      annotations <- annotationQuery
      places <- placeQuery
    } yield (annotations, places.items.map(_._1))

    f.map { case (annotations, places) =>
      val header = Seq("UUID", "QUOTE_TRANSCRIPTION", "ANCHOR", "TYPE", "URI", "VOCAB_LABEL", "LAT", "LNG", "VERIFICATION_STATUS")
      
      val tmp = new TemporaryFile(new File(TMP_DIR, UUID.randomUUID + ".csv"))
      val writer = tmp.file.asCsvWriter[Seq[String]](';', header)
      
      val tupled = sort(annotations.map(_._1)).map(a => serializeOne(a, places))
      tupled.foreach(t => writer.write(t))
      writer.close()
      
      tmp.file
    }
  }

}

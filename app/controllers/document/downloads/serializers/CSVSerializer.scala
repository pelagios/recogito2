package controllers.document.downloads.serializers

import models.annotation.{Annotation, AnnotationService}
import models.place.{Place, PlaceService}
import scala.concurrent.ExecutionContext

trait CSVSerializer extends BaseSerializer {

  private val SEPARATOR = ";"
  private val NEWLINE   = "\n"
  private val EMPTY     = ""

  def annotationsToCSV(documentId: String)(implicit annotationService: AnnotationService, placeService: PlaceService, ctx: ExecutionContext) = {

    def serializeOne(a: Annotation, places: Seq[Place]) = {
      val firstEntity = getFirstEntityBody(a)
      val maybePlace = firstEntity.flatMap(_.uri.flatMap { uri =>
        places.find(_.uris.contains(uri))
      })

      a.annotationId + SEPARATOR +
      getFirstQuote(a).getOrElse(EMPTY) + SEPARATOR +
      a.anchor + SEPARATOR +
      firstEntity.map(_.hasType.toString).getOrElse(EMPTY) + SEPARATOR +
      firstEntity.flatMap(_.uri).getOrElse(EMPTY) + SEPARATOR +
      maybePlace.map(_.labels.mkString(",")).getOrElse(EMPTY) + SEPARATOR +
      maybePlace.flatMap(_.representativePoint.map(_.y)).getOrElse(EMPTY) + SEPARATOR +
      maybePlace.flatMap(_.representativePoint.map(_.x)).getOrElse(EMPTY) + SEPARATOR +
      firstEntity.flatMap(_.status.map(_.value)).getOrElse(EMPTY) + SEPARATOR
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
      val header = Seq("UUID", "QUOTE", "ANCHOR", "TYPE", "URI", "VOCAB_LABEL", "LAT", "LNG", "VERIFICATION_STATUS")
      val serialized = sortByCharOffset(annotations.map(_._1)).map(a => serializeOne(a, places))
      header.mkString(SEPARATOR) + NEWLINE +
      serialized.mkString(NEWLINE)
    }
  }

}

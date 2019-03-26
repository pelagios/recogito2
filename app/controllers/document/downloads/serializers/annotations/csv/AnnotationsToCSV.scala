package controllers.document.downloads.serializers.annotations.csv

import controllers.HasCSVParsing
import controllers.document.downloads.serializers.BaseSerializer
import java.nio.file.Paths
import java.util.UUID
import kantan.csv.CsvConfiguration
import kantan.csv.CsvConfiguration.{Header, QuotePolicy}
import kantan.csv.ops._
import kantan.csv.engine.commons._
import play.api.Configuration
import play.api.libs.Files.TemporaryFileCreator
import scala.concurrent.{ExecutionContext, Future}
import services.annotation.{Annotation, AnnotationBody, AnnotationService}
import services.document.ExtendedDocumentMetadata
import services.entity.{Entity, EntityType}
import services.entity.builtin.EntityService
import storage.TempDir

trait AnnotationsToCSV extends BaseSerializer with HasCSVParsing { 

  private val EMPTY = ""
  
  private def findPlace(body: AnnotationBody, places: Seq[Entity]): Option[Entity] =
    body.uri.flatMap { uri =>
      places.find(_.uris.contains(uri))
    }

  def annotationsToCSV(doc: ExtendedDocumentMetadata)(
    implicit annotationService: AnnotationService,
             entityService: EntityService, 
             tmpFile: TemporaryFileCreator,
             conf: Configuration,
             ctx: ExecutionContext
  ) = {

    def serializeOne(a: Annotation, filename: String, places: Seq[Entity]): Seq[String] = {
      val firstEntity = getFirstEntityBody(a)
      val maybePlace = firstEntity.flatMap(body => findPlace(body, places))
      
      val quoteOrTranscription =
        if (a.annotates.contentType.isText)
          getFirstQuote(a)
        else if (a.annotates.contentType.isImage)
          getFirstTranscription(a)
        else None
        
      val placeTypes = maybePlace.map(_.subjects.map(_._1).mkString(","))

      Seq(a.annotationId.toString,
          filename,
          quoteOrTranscription.getOrElse(EMPTY),
          a.anchor,
          firstEntity.map(_.hasType.toString).getOrElse(EMPTY),
          firstEntity.flatMap(_.uri).getOrElse(EMPTY),
          maybePlace.map(_.titles.mkString("|")).getOrElse(EMPTY),
          maybePlace.flatMap(_.representativePoint.map(_.y.toString)).getOrElse(EMPTY),
          maybePlace.flatMap(_.representativePoint.map(_.x.toString)).getOrElse(EMPTY),
          maybePlace.map(_.subjects.map(_._1).mkString(",")).getOrElse(EMPTY),
          firstEntity.flatMap(_.status.map(_.value.toString)).getOrElse(EMPTY),
          getTagBodies(a).flatMap(_.value).mkString("|"),
          getCommentBodies(a).flatMap(_.value).mkString("|"))
    }

    val fAnnotationsByPart = Future.sequence {
      doc.fileparts.map { part => 
        annotationService.findByFilepartId(part.getId).map { annotationsWithId => 
          (part, annotationsWithId.map(_._1))
        }
      }
    }

    val fPlaces = entityService.listEntitiesInDocument(doc.id, Some(EntityType.PLACE))

    val f = for {
      annotationByPart <- fAnnotationsByPart
      places <- fPlaces
    } yield (annotationByPart, places.items.map(_._1.entity))

    f.map { case (annotationsByPart, places) =>
      scala.concurrent.blocking {
        val header = Seq("UUID", "FILE", "QUOTE_TRANSCRIPTION", "ANCHOR", "TYPE", "URI", "VOCAB_LABEL", "LAT", "LNG", "PLACE_TYPE", "VERIFICATION_STATUS", "TAGS", "COMMENTS")
        
        val tmp = tmpFile.create(Paths.get(TempDir.get(), s"${UUID.randomUUID}.csv"))
        val underlying = tmp.path.toFile
        val config = CsvConfiguration(',', '"', QuotePolicy.Always, Header.Explicit(header))
        val writer = underlying.asCsvWriter[Seq[String]](config)

        val sorted = annotationsByPart.flatMap { case (part, annotations) => 
          sort(annotations).map { (_, part.getTitle) }
        }

        val tupled = sorted.map(t => serializeOne(t._1, t._2, places))
        tupled.foreach(t => writer.write(t))
        writer.close()
        
        underlying
      }
    }
  }
  
}

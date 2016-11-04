package controllers.my.upload.ner

import akka.actor.Actor
import controllers.my.upload.ProgressStatus
import java.io.File
import java.util.UUID
import models.ContentType
import models.annotation._
import models.place.PlaceService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.io.Source
import storage.ES

import org.pelagios.recogito.sdk.ner._

private[ner] object NERWorkerActor {

  val SUPPORTED_CONTENT_TYPES = Set(ContentType.TEXT_PLAIN).map(_.toString)

}

private[ner] class NERWorkerActor(document: DocumentRecord, part: DocumentFilepartRecord, documentDir: File, annotationService: AnnotationService, placeService: PlaceService) extends Actor {

  import controllers.my.upload.ProcessingTaskMessages._

  var progress = 0.0
  var status = ProgressStatus.PENDING

  def receive = {

    case Start => {
      status = ProgressStatus.IN_PROGRESS
      val origSender = sender
      
      parseFilepart(document, part, documentDir).map { phrases =>
        // val entities = phrases.filter(p => (p.entityTag == "LOCATION" || p.entityTag == "PERSON"))
        // TODO temporarily disabling PERSON tags for first pre-release
        val entities = phrases.filter(p => p.entityType == EntityType.LOCATION)
        resolve(entities).map { annotations =>
          annotationService.insertOrUpdateAnnotations(annotations).map { result =>
            progress = 1.0
            status = ProgressStatus.COMPLETED
            if (result.size == 0)
              origSender ! Completed
            else
              origSender ! Failed
          }
        }
      }.recover { case t => {
        t.printStackTrace
        status = ProgressStatus.FAILED
        origSender ! Failed(t.getMessage)
      }}
    }

    case QueryProgress =>
      sender ! WorkerProgress(part.getId, status, progress)

  }

  /** Select appropriate parser for part content type **/
  private def parseFilepart(document: DocumentRecord, part: DocumentFilepartRecord, documentDir: File) =
    part.getContentType match {
      case t if t == ContentType.TEXT_PLAIN.toString =>
        parsePlaintext(document, part, new File(documentDir, part.getFile))

      case t => {
        Logger.info("Skipping NER for file of unsupported type " + t + ": " + documentDir.getName + File.separator + part.getFile)
        Future { Seq.empty[Entity] }
      }
    }

  private def parsePlaintext(document: DocumentRecord, part: DocumentFilepartRecord, file: File) = {
    val text = Source.fromFile(file).getLines.mkString("\n")
    NERService.parse(text)
  }
  
  private def resolve(entities: Seq[Entity]): Future[Seq[Annotation]] =
    // Chaining futures to resolve annotation sequentially
    entities.foldLeft(Future.successful(Seq.empty[Annotation])) { case (future, entity) => 
      future.flatMap { annotations =>
        val fAnnotation = 
          if (entity.entityType == EntityType.LOCATION) {
            placeService.searchPlaces(entity.chars, 0, 1).map { topHits =>
              if (topHits.total > 0)
                // TODO be smarter about choosing the right URI from the place
                toAnnotation(entity, AnnotationBody.PLACE, Some(topHits.items(0)._1.id))
              else
                // No gazetteer match found
                toAnnotation(entity, AnnotationBody.PLACE)
            }
          } else {       
            Future.successful(toAnnotation(entity, AnnotationBody.PERSON))
          }
        
        fAnnotation.map(annotation => annotations :+ annotation)
      }
    }
  
  private def toAnnotation(entity: Entity, annotationType: AnnotationBody.Value, uri: Option[String] = None): Annotation = {
    val now = DateTime.now
    
    Annotation(
      UUID.randomUUID,
      UUID.randomUUID,
      AnnotatedObject(document.getId, part.getId, ContentType.withName(part.getContentType).get),
      Seq.empty[String], // No contributing users
      "char-offset:" + entity.charOffset,
      None, // no last modifying user
      now,
      Seq(
        AnnotationBody(
          AnnotationBody.QUOTE,
          None,  // no last modifying user
          now,
          Some(entity.chars),
          None,  // uri
          None), // status
        AnnotationBody(
          annotationType,
          None,
          now,
          None,
          uri,
          Some(AnnotationStatus(
            AnnotationStatus.UNVERIFIED,
            None,
            now)))))
  }

}

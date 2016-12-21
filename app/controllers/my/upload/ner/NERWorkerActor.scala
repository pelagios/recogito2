package controllers.my.upload.ner

import akka.actor.Actor
import java.io.File
import java.util.UUID
import models.ContentType
import models.annotation._
import models.place.PlaceService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import models.task.{ TaskStatus, TaskService }
import org.joda.time.DateTime
import org.pelagios.recogito.sdk.ner._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.io.Source
import storage.ES

private[ner] object NERWorkerActor {

  val SUPPORTED_CONTENT_TYPES = Set(ContentType.TEXT_PLAIN).map(_.toString)

}

private[ner] class NERWorkerActor(
    document: DocumentRecord,
    part: DocumentFilepartRecord,
    documentDir: File, 
    taskService: TaskService,
    annotationService: AnnotationService,
    placeService: PlaceService
  ) extends Actor {

  import controllers.my.upload.ProcessingMessages._
    
  def receive = {

    case Start => {
      val origSender = sender

      val taskId = Await.result(
        taskService.insertTask(
          NERService.TASK_NER.toString,
          this.getClass.getName,
          Some(document.getId),
          Some(part.getId),
          Some(document.getOwner)),
        10.seconds)
        
      taskService.updateStatusAndProgress(taskId, TaskStatus.RUNNING, 1)

      parseFilepart(document, part, documentDir).map { phrases =>
        
        taskService.updateProgress(taskId, 50)
        
        val entities = phrases.filter(p => p.entityType == EntityType.LOCATION || p.entityType == EntityType.PERSON)        
        resolve(entities).map { annotations =>
          annotationService.insertOrUpdateAnnotations(annotations).map { result =>            
            if (result.size == 0)
              taskService.setCompleted(taskId)
            else
              taskService.setFailed(taskId, Some("Failed to insert " + result.size + " annotations"))
       
            origSender ! Stopped
          }
        }
      }.recover { case t => {
        t.printStackTrace
        
        taskService.setFailed(taskId, Some(t.getMessage))
        
        origSender ! Stopped
      }}
    }

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
            placeService.searchPlaces(ES.sanitize(entity.chars), 0, 1).map { topHits =>
              if (topHits.total > 0)
                // TODO be smarter about choosing the right URI from the place
                toAnnotation(entity, AnnotationBody.PLACE, Some(topHits.items(0)._1.id))
              else
                // No gazetteer match found
                toAnnotation(entity, AnnotationBody.PLACE)
            }.recover { case t: Throwable =>
              t.printStackTrace()
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

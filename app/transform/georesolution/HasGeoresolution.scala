package transform.georesolution

import com.vividsolutions.jts.geom.Coordinate
import java.util.UUID
import services.ContentType
import services.annotation._
import services.entity.{EntityService, EntityType}
import services.task.TaskService
import services.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.joda.time.DateTime
import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._
import storage.es.ES

trait Georesolvable {
  
  val toponym: String
  
  val coord: Option[Coordinate]
  
}

object Georesolvable {
  
  private[Georesolvable] class DefaultGeoresolvable(val toponym: String, val coord: Option[Coordinate]) extends Georesolvable
 
  def apply(toponym: String, coord: Option[Coordinate]) =
    new DefaultGeoresolvable(toponym, coord)
  
}


/** Separating this out, so we can re-use in the NER service **/
trait HasGeoresolution {
  
  type T <: Georesolvable
  
  def getAnchor(resolvable: T, index: Int): String
    
  protected def resolve(
      document: DocumentRecord,
      part: DocumentFilepartRecord,
      resolvables: TraversableOnce[Option[T]],
      total: Int,
      taskId: UUID,
      progressRange: (Int, Int) = (0, 100)
    )(implicit annotationService: AnnotationService, entityService: EntityService, taskService: TaskService, ctx: ExecutionContext) = {
    
    val docId = document.getId
    val partId = part.getId
    val contentType = ContentType.withName(part.getContentType).get
    
    def resolveOne(resolvable: T, index: Int) = {
      entityService.searchEntities(ES.sanitize(resolvable.toponym), Some(EntityType.PLACE), 0, 1, resolvable.coord).map { topHits =>
        if (topHits.total > 0)
          // TODO be smarter about choosing the right URI from the place
          toAnnotation(docId, partId, contentType, resolvable, Some(topHits.items(0).entity.uris.head), index)         
        else
          // No gazetteer match found
          toAnnotation(docId, partId, contentType, resolvable, None, index)
      }.recover { case t: Throwable =>
        t.printStackTrace()
        toAnnotation(docId, partId, contentType, resolvable, None, index)
      }
    }
    
    var counter = 0
    var progress = progressRange._1
    
    resolvables.foreach { maybeResolvable =>
      
      maybeResolvable match {
        case Some(resolvable) =>
          val f = for {
            annotation <- resolveOne(resolvable, counter)
            (success, _) <- annotationService.upsertAnnotation(annotation)
          } yield (success)
          
          Await.result(f, 10.seconds)
          
        case None => // Skip
      }
      
      counter += 1
      val p = progressRange._1 + (progressRange._2 - progressRange._1) * counter / total
      if (p > progress) {
        taskService.updateProgress(taskId, p)
        progress = p
      } 
    }
    
  }
  
  private def toAnnotation(
      documentId: String,
      partId: UUID,
      contentType: ContentType,
      resolvable: T,
      uri: Option[String],
      index: Int
    ): Annotation = {
    
    val now = new DateTime()
    Annotation(
      UUID.randomUUID,
      UUID.randomUUID,
      AnnotatedObject(documentId, partId, contentType),
      Seq.empty[String], // no contributors
      getAnchor(resolvable, index),
      None, // no last modifying user
      now,
      Seq(
        AnnotationBody(
          AnnotationBody.QUOTE,
          None, // no last modifying user
          now,
          Some(resolvable.toponym),
          None,
          None, // note
          None
        ),
        AnnotationBody(
          AnnotationBody.PLACE,
          None,
          now,
          None,
          uri,
          None, // note
          Some(AnnotationStatus(AnnotationStatus.UNVERIFIED, None,now))
        )
      )
    )
  }
  
}
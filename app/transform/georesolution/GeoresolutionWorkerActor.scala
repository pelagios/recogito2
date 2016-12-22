package transform.georesolution

import akka.actor.Actor
import java.io.File
import kantan.csv.ops._
import kantan.codecs.Result.Success
import models.annotation.AnnotationService
import models.place.PlaceService
import models.task.{ TaskService, TaskStatus }
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._
import storage.ES

private[georesolution] class GeoresolutionWorkerActor(
    document: DocumentRecord,
    part: DocumentFilepartRecord,
    documentDir: File, 
    taskService: TaskService,
    annotationService: AnnotationService,
    placeService: PlaceService,
    implicit val ctx: ExecutionContext) extends Actor {    
  
  import transform.TransformTaskMessages._ 
  
  def receive = {

    case Start => {
      val origSender = sender
      
      val taskId = Await.result(
        taskService.insertTask(
          GeoresolutionService.TASK_TYPE,
          this.getClass.getName,
          Some(document.getId),
          Some(part.getId),
          Some(document.getOwner)),
        10.seconds)
        
      taskService.updateStatusAndProgress(taskId, TaskStatus.RUNNING, 1)
      
      // TODO get separate from args
      val totalResolvablePlaces = parse().size
      play.api.Logger.info("total: " + totalResolvablePlaces)
      resolve(parse(), totalResolvablePlaces)
    }

  }
  
  private def parse() = new File(documentDir, part.getFile).asCsvReader[List[String]](sep = ',', header = true).map {
    case Success(line) =>
      val toponym = line(9).trim()
      if (toponym.size > 0)
        Some(Georesolvable(toponym, None))
      else
        None
        
    case _ => None
  }.filter(_.isDefined).flatten
  
  private def resolve(resolvables: TraversableOnce[Georesolvable], total: Int) = {
    var counter = 0.0
    
    def resolveOne(resolvable: Georesolvable) =
      placeService.searchPlaces(ES.sanitize(resolvable.toponym), 0, 1).map { topHits =>
        if (topHits.total > 0)
          // TODO be smarter about choosing the right URI from the place
          // toAnnotation(entity, AnnotationBody.PLACE, Some(topHits.items(0)._1.id))
          play.api.Logger.info(topHits.items(0)._1.id)
        else
          // No gazetteer match found
          // toAnnotation(entity, AnnotationBody.PLACE)
          play.api.Logger.info("no match found")
      }.recover { case t: Throwable =>
        t.printStackTrace()
        // toAnnotation(entity, AnnotationBody.PLACE)
      } map { _ => 
        counter += 1 
        play.api.Logger.info((100 * counter / total) + "%") 
      }
    
    resolvables.foreach(resolvable =>
      Await.result(resolveOne(resolvable), 10.seconds))
    
  }
  
}
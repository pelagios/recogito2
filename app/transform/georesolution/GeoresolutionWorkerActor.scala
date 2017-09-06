package transform.georesolution

import akka.actor.Actor
import com.vividsolutions.jts.geom.Coordinate
import java.io.File
import kantan.csv.CsvConfiguration
import kantan.csv.CsvConfiguration.{ Header, QuotePolicy }
import kantan.csv.ops._
import kantan.csv.engine.commons._
import kantan.codecs.Result.Success
import models.annotation.AnnotationService
import models.place.PlaceService
import models.task.{ TaskService, TaskStatus }
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._
import scala.util.Try

private[georesolution] class GeoresolutionWorkerActor(
    document: DocumentRecord,
    part: DocumentFilepartRecord,
    documentDir: File, 
    args: Map[String, String],
    implicit val taskService: TaskService,
    implicit val annotationService: AnnotationService,
    implicit val placeService: PlaceService,
    implicit val ctx: ExecutionContext) extends Actor with HasGeoresolution {    
  
  type T = Georesolvable
  
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
      
      try {
        val totalResolvablePlaces = parse().size
        resolve(document, part, parse(), totalResolvablePlaces, taskId)        
        taskService.setCompleted(taskId)
        origSender ! Stopped
      } catch { case t: Throwable =>
        t.printStackTrace()
        taskService.setFailed(taskId, Some(t.getMessage))
        origSender ! Stopped
      }
    }
  }
  
  private def parse() = {
    val delimiter = args.get("delimiter").map(_.charAt(0)).getOrElse(',')
    val config = CsvConfiguration(delimiter, '"', QuotePolicy.WhenNeeded, Header.Implicit)
    val toponymColumn = args.get("toponym_column").get.toInt // Must be set - if not, fail
    
    val latColumn = args.get("lat_column").map(_.toInt)
    val lonColumn = args.get("lon_column").map(_.toInt)
    val hasCoordHint = latColumn.isDefined && lonColumn.isDefined
    
    new File(documentDir, part.getFile).asCsvReader[List[String]](config).map {
      case Success(line) =>
        val toponym = line(toponymColumn).trim()
        
        if (toponym.size > 0) {
          val coord =
            if (hasCoordHint) {
              val lat = line(latColumn.get).trim().replace(",", ".")
              val lon = line(lonColumn.get).trim().replace(",", ".")
              Try(new Coordinate(lon.toDouble, lat.toDouble)) match {
                case scala.util.Success(pt) => Some(pt)
                case _ => None
              }
            } else {
              None
            }
          
          Some(Georesolvable(toponym, coord))
        } else {
          None
        }
          
      case _ => None
    }
  }
  
  override def getAnchor(resolvable: Georesolvable, index: Int) = "row:" + index
  
}
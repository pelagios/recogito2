package transform.georesolution

import akka.actor.Props
import com.vividsolutions.jts.geom.Coordinate
import java.io.File
import java.util.UUID
import kantan.csv.CsvConfiguration
import kantan.csv.CsvConfiguration.{Header, QuotePolicy}
import kantan.csv.ops._
import kantan.csv.engine.commons._
import kantan.codecs.Result.Success
import scala.util.Try
import services.annotation.AnnotationService
import services.entity.builtin.EntityService
import services.task.TaskService
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import transform.WorkerActor

class GeoresolutionActor(
  implicit val taskService: TaskService, 
  implicit val annotationService: AnnotationService, 
  implicit val entityService: EntityService
) extends WorkerActor(GeoresolutionService.TASK_TYPE, taskService) with HasGeoresolution {
  
  type T = Georesolvable
  
  private implicit val ctx = context.dispatcher
  
  def doWork(doc: DocumentRecord, part: DocumentFilepartRecord, dir: File, args: Map[String, String], taskId: UUID) =
    try {
      val toponyms = parse(part, dir, args).toSeq
      resolve(doc, part, toponyms, toponyms.size, taskId)
      taskService.setCompleted(taskId)
    } catch { case t: Throwable =>
      t.printStackTrace()
      taskService.setFailed(taskId, Some(t.getMessage))
    }
  
  private def parse(part: DocumentFilepartRecord, dir: File, args: Map[String, String]) = {
    val delimiter = args.get("delimiter").map(_.charAt(0)).getOrElse(',')
    val config = CsvConfiguration(delimiter, '"', QuotePolicy.WhenNeeded, Header.Implicit)
    val toponymColumn = args.get("toponym_column").get.toInt // Must be set - if not, fail
    
    val latColumn = args.get("lat_column").map(_.toInt)
    val lonColumn = args.get("lon_column").map(_.toInt)
    val hasCoordHint = latColumn.isDefined && lonColumn.isDefined
    
    var counter = -1
    new File(dir, part.getFile).asCsvReader[List[String]](config).map {
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
          
          counter += 1
          Some(Georesolvable(toponym, s"row:${counter}", coord))
        } else {
          None
        }
          
      case _ => None
    }
  }
  
}

object GeoresolutionActor {
  
  def props(taskService: TaskService, annotationService: AnnotationService, entityService: EntityService) =
    Props(classOf[GeoresolutionActor], taskService, annotationService, entityService)

}
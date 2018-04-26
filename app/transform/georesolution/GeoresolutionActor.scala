package transform.georesolution

import akka.actor.{Actor, Props}
import com.vividsolutions.jts.geom.Coordinate
import java.io.File
import kantan.csv.CsvConfiguration
import kantan.csv.CsvConfiguration.{Header, QuotePolicy}
import kantan.csv.ops._
import kantan.csv.engine.commons._
import kantan.codecs.Result.Success
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import services.annotation.AnnotationService
import services.entity.builtin.EntityService
import services.task.{TaskService, TaskStatus}
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}

class GeoresolutionActor(
  implicit val taskService: TaskService, 
  implicit val annotationService: AnnotationService, 
  implicit val entityService: EntityService
) extends Actor with HasGeoresolution {
  
  type T = Georesolvable
  
  private implicit val ctx = context.dispatcher
  
  def receive = {
    
    case msg: GeoresolutionActor.ResolveData =>
      val taskId = Await.result(
        taskService.insertTask(
          GeoresolutionService.TASK_TYPE,
          this.getClass.getName,
          Some(msg.document.getId),
          Some(msg.part.getId),
          Some(msg.document.getOwner)),
        10.seconds)
        
      taskService.updateStatusAndProgress(taskId, TaskStatus.RUNNING, 1)
      
      try {
        val toponyms = parse(msg.part, msg.dir, msg.args).toSeq
        resolve(msg.document, msg.part, toponyms, toponyms.size, taskId)
        taskService.setCompleted(taskId)
      } catch { case t: Throwable =>
        t.printStackTrace()
        taskService.setFailed(taskId, Some(t.getMessage))
      }
      
      taskService.scheduleForRemoval(taskId, 10.seconds)(context.system)
    
  }
  
  private def parse(part: DocumentFilepartRecord, dir: File, args: Map[String, String]) = {
    val delimiter = args.get("delimiter").map(_.charAt(0)).getOrElse(',')
    val config = CsvConfiguration(delimiter, '"', QuotePolicy.WhenNeeded, Header.Implicit)
    val toponymColumn = args.get("toponym_column").get.toInt // Must be set - if not, fail
    
    val latColumn = args.get("lat_column").map(_.toInt)
    val lonColumn = args.get("lon_column").map(_.toInt)
    val hasCoordHint = latColumn.isDefined && lonColumn.isDefined
    
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
          
          Some(Georesolvable(toponym, coord))
        } else {
          None
        }
          
      case _ => None
    }
  }
  
  override def getAnchor(resolvable: Georesolvable, index: Int) = "row:" + index
  
}

object GeoresolutionActor {
  
  def props(taskService: TaskService, annotationService: AnnotationService, entityService: EntityService) =
    Props(classOf[GeoresolutionActor], taskService, annotationService, entityService)
    
  case class ResolveData(
    document : DocumentRecord,
    part     : DocumentFilepartRecord,
    dir      : File,
    args     : Map[String, String]) 

}
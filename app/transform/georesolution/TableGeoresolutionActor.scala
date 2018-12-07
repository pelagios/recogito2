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
import transform.{WorkerActor, SpecificJobDefinition}

class TableGeoresolutionActor(
  implicit val taskService: TaskService, 
  implicit val annotationService: AnnotationService, 
  implicit val entityService: EntityService
) extends WorkerActor(GeoresolutionService.TASK_TYPE, taskService) with HasGeoresolution {
  
  type T = Georesolvable
  
  private implicit val ctx = context.dispatcher
  
  def doWork(
    doc: DocumentRecord, 
    part: DocumentFilepartRecord, 
    dir: File, 
    jobDef: Option[SpecificJobDefinition], 
    taskId: UUID
  ) = try {
    val definition = jobDef.get.asInstanceOf[TableGeoresolutionJobDefinition]
    val toponyms = parse(part, dir, definition).toSeq
    resolve(doc, part, toponyms, toponyms.size, taskId)
    taskService.setTaskCompleted(taskId)
  } catch { case t: Throwable =>
    t.printStackTrace()
    taskService.setTaskFailed(taskId, Some(t.getMessage))
  }
  
  private def parse(part: DocumentFilepartRecord, dir: File, jobDef: TableGeoresolutionJobDefinition) = {
    val delimiter = jobDef.delimiter.getOrElse(',')
    val config = CsvConfiguration(delimiter, '"', QuotePolicy.WhenNeeded, Header.Implicit)
    val hasCoordHint = jobDef.latitudeColumn.isDefined && jobDef.longitudeColumn.isDefined
    
    var counter = -1
    new File(dir, part.getFile).asCsvReader[List[String]](config).map {
      case Success(line) =>
        val toponym = line(jobDef.toponymColumn).trim()        
        if (toponym.size > 0) {
          val coord =
            if (hasCoordHint) {
              val lat = line(jobDef.latitudeColumn.get).trim().replace(",", ".")
              val lon = line(jobDef.longitudeColumn.get).trim().replace(",", ".")
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
    Props(classOf[TableGeoresolutionActor], taskService, annotationService, entityService)

}
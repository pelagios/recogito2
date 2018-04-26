package transform.ner

import akka.actor.Props
import java.io.File
import java.util.UUID
import org.pelagios.recogito.sdk.ner._
import play.api.Logger
import scala.io.Source
import services.ContentType
import services.annotation.AnnotationService
import services.entity.builtin.EntityService
import services.task.TaskService
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import transform.georesolution.{Georesolvable, HasGeoresolution}
import transform.WorkerActor

case class EntityGeoresolvable(entity: Entity) extends Georesolvable {
  val toponym = entity.chars
  val coord = None
}

class NERActor(
  implicit val taskService: TaskService,
  implicit val annotationService: AnnotationService,
  implicit val entityService: EntityService
) extends WorkerActor(NERService.TASK_TYPE, taskService) with HasGeoresolution {
  
  type T = EntityGeoresolvable
  
  private implicit val ctx = context.dispatcher
  
  override def doWork(doc: DocumentRecord, part: DocumentFilepartRecord, dir: File, args: Map[String, String], taskId: UUID) =
    try {
      Logger.info(s"Starting NER on ${part.getId}")
      val phrases = parseFilepart(doc, part, dir)
      
      Logger.info(s"NER completed on ${part.getId}")
      taskService.updateProgress(taskId, 50)
      
      val places = phrases.filter(_.entityType == EntityType.LOCATION).map(e => Some(EntityGeoresolvable(e)))
      val persons = phrases.filter(_.entityType == EntityType.PERSON)     
      
      resolve(doc, part, places, places.size, taskId, (50, 80))
      
      // TODO store person annotations
      
      taskService.setCompleted(taskId)
    } catch { case t: Throwable =>
      t.printStackTrace()
      taskService.setFailed(taskId, Some(t.getMessage))
    }
    
  /** Select appropriate parser for part content type **/
  private def parseFilepart(document: DocumentRecord, part: DocumentFilepartRecord, dir: File) =
    part.getContentType match {
      case t if t == ContentType.TEXT_PLAIN.toString =>
        val file =  new File(dir, part.getFile)
        val text = Source.fromFile(file).getLines.mkString("\n")
        NERService.parse(text)

      case t =>
        Logger.info(s"Skipping NER for file of unsupported type ${t}: ${dir.getName}${File.separator}${part.getFile}")
        Seq.empty[Entity]
    }
  
  override def getAnchor(resolvable: EntityGeoresolvable, index: Int) =
    "char-offset:" + resolvable.entity.charOffset
  
}

object NERActor {
  
  def props(taskService: TaskService, annotationService: AnnotationService, entityService: EntityService) = 
    Props(classOf[NERActor], taskService, annotationService, entityService)

}
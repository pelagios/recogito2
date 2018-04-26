package transform.ner

import akka.actor.{Actor, Props}
import java.io.File
import org.pelagios.recogito.sdk.ner._
import play.api.Logger
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import services.ContentType
import services.annotation.AnnotationService
import services.entity.builtin.EntityService
import services.task.{TaskService, TaskStatus}
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import transform.georesolution.{Georesolvable, HasGeoresolution}

case class EntityGeoresolvable(entity: Entity) extends Georesolvable {
  val toponym = entity.chars
  val coord = None
}

class NERActor(
  implicit val taskService: TaskService,
  implicit val annotationService: AnnotationService,
  implicit val entityService: EntityService
) extends Actor with HasGeoresolution {
  
  type T = EntityGeoresolvable
  
  private implicit val ctx = context.dispatcher
  
  def receive = {
    
    case msg: NERActor.ProcessText =>
      
      val taskId = Await.result(
        taskService.insertTask(
          NERService.TASK_TYPE,
          this.getClass.getName,
          Some(msg.document.getId),
          Some(msg.part.getId),
          Some(msg.document.getOwner)),
        10.seconds)
        
      taskService.updateStatusAndProgress(taskId, TaskStatus.RUNNING, 1)
      
      try {
        Logger.info(s"Starting NER on ${msg.part.getId}")
        val phrases = parseFilepart(msg.document, msg.part, msg.dir)
        
        Logger.info(s"NER completed on ${msg.part.getId}")
        taskService.updateProgress(taskId, 50)
        
        val places = phrases.filter(_.entityType == EntityType.LOCATION).map(e => Some(EntityGeoresolvable(e)))
        val persons = phrases.filter(_.entityType == EntityType.PERSON)     
        
        resolve(msg.document, msg.part, places, places.size, taskId, (50, 80))
        
        // TODO store person annotations
        
        taskService.setCompleted(taskId)
      } catch { case t: Throwable =>
        t.printStackTrace()
        taskService.setFailed(taskId, Some(t.getMessage))
      } 
      
      taskService.scheduleForRemoval(taskId, 10.seconds)(context.system)
      
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
    
  case class ProcessText(
    document : DocumentRecord,
    part     : DocumentFilepartRecord,
    dir      : File) 

}
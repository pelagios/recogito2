package transform.ner

import akka.actor.Actor
import java.io.File
import java.util.UUID
import services.ContentType
import services.annotation._
import services.entity.builtin.EntityService
import services.task._
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import org.joda.time.DateTime
import org.pelagios.recogito.sdk.ner._
import play.api.Logger
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.io.Source
import storage.es.ES
import transform.georesolution.{Georesolvable, HasGeoresolution}

case class EntityGeoresolvable(entity: Entity) extends Georesolvable {
  
  val toponym = entity.chars
  
  val coord = None
  
}

private[ner] class NERWorkerActor(
    document: DocumentRecord,
    part: DocumentFilepartRecord,
    documentDir: File, 
    args: Map[String, String],
    implicit val taskService: TaskService,
    implicit val annotationService: AnnotationService,
    implicit val entityService: EntityService,
    implicit val ctx: ExecutionContext) extends Actor with HasGeoresolution {

  import transform.TransformTaskMessages._ 
  
  type T = EntityGeoresolvable
    
  def receive = {

    case Start => {
      val origSender = sender

      val taskId = Await.result(
        taskService.insertTask(
          NERService.TASK_TYPE,
          this.getClass.getName,
          Some(document.getId),
          Some(part.getId),
          Some(document.getOwner)),
        10.seconds)
        
      taskService.updateStatusAndProgress(taskId, TaskStatus.RUNNING, 1)

      parseFilepart(document, part, documentDir).map { phrases =>
        taskService.updateProgress(taskId, 50)
        
        val places = phrases.filter(_.entityType == EntityType.LOCATION).map(e => Some(EntityGeoresolvable(e)))
        val persons = phrases.filter(_.entityType == EntityType.PERSON)     
        
        try {
          resolve(document, part, places, places.size, taskId, (50, 80))
          
          // TODO store person annotations
          
          taskService.setCompleted(taskId)
          origSender ! Stopped
        } catch { case t: Throwable =>
          t.printStackTrace()
          taskService.setFailed(taskId, Some(t.getMessage))
          origSender ! Stopped
        }        
      }
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
  
  override def getAnchor(resolvable: EntityGeoresolvable, index: Int) =
    "char-offset:" + resolvable.entity.charOffset
}

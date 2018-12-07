package transform.ner

import akka.actor.Props
import java.io.File
import java.net.URI
import java.util.UUID
import org.pelagios.recogito.sdk.ner._
import play.api.Logger
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import services.ContentType
import services.annotation.{Annotation, AnnotationBody, AnnotationService}
import services.entity.builtin.EntityService
import services.task.TaskService
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import transform.georesolution.{Georesolvable, HasGeoresolution}
import transform.{WorkerActor, SpecificJobDefinition}

case class EntityResolvable(entity: Entity, val anchor: String, val uri: Option[URI]) extends Georesolvable {
  val toponym = entity.chars
  val coord = None
}

class NERActor(
  implicit val taskService: TaskService,
  implicit val annotationService: AnnotationService,
  implicit val entityService: EntityService
) extends WorkerActor(NERService.TASK_TYPE, taskService) with HasGeoresolution {
  
  type T = EntityResolvable
  
  private implicit val ctx = context.dispatcher
  
  override def doWork(
    doc: DocumentRecord, 
    part: DocumentFilepartRecord, 
    dir: File, 
    jobDef: Option[SpecificJobDefinition], 
    taskId: UUID
  ) = try {
    Logger.info(s"Starting NER on ${part.getId}")
    Logger.info(s"definition: ${jobDef.get}")
    val phrases = parseFilepart(doc, part, dir)
    
    Logger.info(s"NER completed on ${part.getId}")
    taskService.updateTaskProgress(taskId, 50)
    
    val places = phrases.filter(_.entity.entityType == EntityType.LOCATION).map(Some(_))
    val persons = phrases.filter(_.entity.entityType == EntityType.PERSON)     
    
    resolve(doc, part, places, places.size, taskId, (50, 80))
    
    val fInsertPeople = annotationService.upsertAnnotations(persons.map { r => 
      Annotation
        .on(part, r.anchor)
        .withBody(AnnotationBody.quoteBody(r.entity.chars))
        .withBody(AnnotationBody.personBody())
    })
    Await.result(fInsertPeople, 20.minutes)
    
    taskService.setTaskCompleted(taskId)
  } catch { case t: Throwable =>
    t.printStackTrace()
    taskService.setTaskFailed(taskId, Some(t.getMessage))
  }
    
  /** Select appropriate parser for part content type **/
  private def parseFilepart(document: DocumentRecord, part: DocumentFilepartRecord, dir: File) =
    ContentType.withName(part.getContentType) match {
      case Some(t) if t == ContentType.TEXT_PLAIN =>
        val text = Source.fromFile(new File(dir, part.getFile)).getLines.mkString("\n")
        val entities = NERService.parseText(text)
        entities.map(e => EntityResolvable(e, s"char-offset:${e.charOffset}", Option(e.uri)))
        
      case Some(t) if t == ContentType.TEXT_TEIXML =>
        // For simplicity, NER results are inlined into the TEI document. They
        // will be extracted (together with all pre-existing tags) in a separate
        // step, anyway.
        val entitiesAndAnchors = NERService.parseTEI(new File(dir, part.getFile))
        entitiesAndAnchors.map { case (e, anchor) => 
          EntityResolvable(e, anchor, Option(e.uri))
        }

      case _ =>
        Logger.info(s"Skipping NER for file of unsupported type ${part.getContentType}: ${dir.getName}${File.separator}${part.getFile}")
        Seq.empty[EntityResolvable]
    }
  
}

object NERActor {
  
  def props(taskService: TaskService, annotationService: AnnotationService, entityService: EntityService) = 
    Props(classOf[NERActor], taskService, annotationService, entityService)

}
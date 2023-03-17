package transform.mapkurator

import akka.actor.Props
import java.io.{File, FileInputStream}
import java.nio.file.Files
import java.util.UUID
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json.{Json, JsArray}
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import services.ContentType
import services.annotation.{Annotation, AnnotationBody, AnnotationService}
import services.task.TaskService
import transform.{WorkerActor, SpecificJobDefinition}

class MapKuratorActor(
  taskService: TaskService, 
  annotationService: AnnotationService,
  config: Configuration
) extends WorkerActor(MapKuratorService.TASK_TYPE, taskService) {

  override def doWork(
    doc: DocumentRecord, 
    part: DocumentFilepartRecord, 
    dir: File, 
    jobDef: Option[SpecificJobDefinition], 
    taskId: UUID
  ) = {   
    try {
      val result: File = MapKuratorService.callMapkurator(doc, part, dir, jobDef.get.asInstanceOf[MapKuratorJobDefinition], config)

      val json = Json.parse(new FileInputStream(result)).as[JsArray].value

      val annotations: Seq[Annotation] = json.map(obj => {
        val firstBody = (obj \ "body").as[JsArray].value.find { body =>
          (body \ "purpose").asOpt[String] match {
            case Some(value) => value == "transcribing"
            case None => false
          }
        }

        val transcription = firstBody.flatMap { body =>
          (body \ "value").asOpt[String]
        }

        val selector = (obj \ "target" \ "selector").as[JsArray].value.head

        val typ = (selector \ "type").as[String]
        val value = (selector \ "value").as[String]

        typ match {
          case "SvgSelector" => 
            // We're assuming ONLY polygon selectors for now!
            if (transcription.isDefined)
              Annotation
                .on(part, s"svg.polygon:$value")
                .withBody(AnnotationBody(
                  AnnotationBody.TRANSCRIPTION,
                  None, // lastModifiedBy
                  DateTime.now(),
                  transcription,
                  None, // reference
                  None, // note
                  None) // status
                )
            else
              Annotation
                .on(part, s"svg.polygon:$value")

          case typ =>
            play.api.Logger.info(s"Unsupported selector type: $typ")
            throw new Exception(s"Unsupported selector type: $typ")
        }
      })

      play.api.Logger.info(s"Ingesting results ${annotations.size} annotations")

      annotationService.upsertAnnotations(annotations, false)

      Files.delete(result.toPath().toAbsolutePath())

      taskService.setTaskCompleted(taskId)
    } catch { 
      case t: Throwable =>
        t.printStackTrace()
        taskService.setTaskFailed(taskId, Some(t.getMessage()))
    }    
  }
  
}

object MapKuratorActor {
  
  def props(taskService: TaskService, annotationService: AnnotationService, config: Configuration) = 
    Props(classOf[MapKuratorActor], taskService, annotationService, config)

}
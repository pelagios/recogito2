package controllers.my.upload.ner

import akka.actor.Props
import controllers.my.upload.{ TaskSupervisorActor, TaskType }
import java.io.File
import models.annotation.AnnotationService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import models.place.PlaceService
import models.task.TaskService
import scala.concurrent.duration.FiniteDuration

private[ner] class NERSupervisorActor(
    task: TaskType, 
    document: DocumentRecord,
    parts: Seq[DocumentFilepartRecord],
    dir: File,
    taskService: TaskService,
    annotations: AnnotationService,
    places: PlaceService,
    keepalive: FiniteDuration
  ) extends TaskSupervisorActor(task, document, parts, dir, taskService, keepalive) {
  
  /** Creates workers for every content type indicated as 'supported' by the Worker class **/
  override def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File) =
    parts
      .filter(part => NERWorkerActor.SUPPORTED_CONTENT_TYPES.contains(part.getContentType))
      .map(p => context.actorOf(Props(classOf[NERWorkerActor], document, p, dir, taskService, annotations, places), name="ner_doc_" + document.getId + "_part" + p.getId))

}



package transform.ner

import akka.actor.Props
import java.io.File
import models.annotation.AnnotationService
import models.place.PlaceService
import models.task.TaskService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import transform.TransformSupervisorActor

private[ner] class NERSupervisorActor(
    document: DocumentRecord,
    parts: Seq[DocumentFilepartRecord],
    dir: File,
    taskService: TaskService,
    annotations: AnnotationService,
    places: PlaceService,
    keepalive: FiniteDuration,
    ctx: ExecutionContext
  ) extends TransformSupervisorActor(
      NERService.TASK_TYPE,
      document,
      parts,
      dir,
      taskService,
      keepalive,
      ctx) {

  /** Creates workers for every content type indicated as 'supported' by the Worker class **/
  override def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File) =
    parts
      .filter(part => NERService.SUPPORTED_CONTENT_TYPES.contains(part.getContentType))
      .map(part => context.actorOf(
        Props(
          classOf[NERWorkerActor],
          document,
          part,
          dir,
          taskService,
          annotations,
          places,
          ctx),
        name="ner.doc." + document.getId + ".part." + part.getId))

}

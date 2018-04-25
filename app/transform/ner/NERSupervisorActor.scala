package transform.ner

import akka.actor.Props
import java.io.File
import services.annotation.AnnotationService
import services.entity.builtin.EntityService
import services.task.TaskService
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import transform.TransformSupervisorActor

private[ner] class NERSupervisorActor(
    document: DocumentRecord,
    parts: Seq[DocumentFilepartRecord],
    dir: File,
    args: Map[String, String],
    taskService: TaskService,
    annotations: AnnotationService,
    entities: EntityService,
    keepalive: FiniteDuration,
    ctx: ExecutionContext
  ) extends TransformSupervisorActor(
      NERService.TASK_TYPE,
      document,
      parts,
      dir,
      args,
      taskService,
      keepalive,
      ctx) {

  /** Creates workers for every content type indicated as 'supported' by the Worker class **/
  override def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File, args: Map[String, String]) =
    parts
      .filter(part => NERService.SUPPORTED_CONTENT_TYPES.contains(part.getContentType))
      .map(part => context.actorOf(
        Props(
          classOf[NERWorkerActor],
          document,
          part,
          dir,
          args,
          taskService,
          annotations,
          entities,
          ctx),
        name = "ner.doc." + document.getId + ".part." + part.getId))

}
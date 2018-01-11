package transform.tiling

import akka.actor.Props
import java.io.File
import services.ContentType
import services.task.TaskService
import services.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import transform.TransformSupervisorActor

private[tiling] class TilingSupervisorActor(
    document   : DocumentRecord,
    parts      : Seq[DocumentFilepartRecord],
    documentDir: File,
    args       : Map[String, String],
    taskService: TaskService,
    keepalive  : FiniteDuration,
    ctx        : ExecutionContext
  ) extends TransformSupervisorActor(
      TilingService.TASK_TYPE,
      document,
      parts,
      documentDir,
      args,
      taskService,
      keepalive,
      ctx) {
  
  /** Creates workers for every image upload **/
  override def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File, args: Map[String, String]) =
    parts
      .filter(_.getContentType.equals(ContentType.IMAGE_UPLOAD.toString))
      .map(part => context.actorOf(
          Props(
            classOf[TilingWorkerActor],
            document,
            part,
            dir,
            args,
            taskService,
            ctx),
          name = "tile.doc." + document.getId + ".part." + part.getId))

}

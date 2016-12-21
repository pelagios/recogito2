package transform.tiling

import akka.actor.Props
import java.io.File
import models.ContentType
import models.task.TaskService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import transform.TransformSupervisorActor

private[tiling] class TilingSupervisorActor(
    document   : DocumentRecord,
    parts      : Seq[DocumentFilepartRecord],
    documentDir: File,
    taskService: TaskService,
    keepalive  : FiniteDuration,
    ctx        : ExecutionContext
  ) extends TransformSupervisorActor(
      TilingService.TASK_TYPE,
      document,
      parts,
      documentDir,
      taskService,
      keepalive,
      ctx) {
  
  /** Creates workers for every image upload **/
  override def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File) =
    parts
      .filter(_.getContentType.equals(ContentType.IMAGE_UPLOAD.toString))
      .map(part => context.actorOf(
          Props(
            classOf[TilingWorkerActor],
            document,
            part,
            dir,
            taskService,
            ctx),
          name="tile.doc." + document.getId + ".part." + part.getId))

}

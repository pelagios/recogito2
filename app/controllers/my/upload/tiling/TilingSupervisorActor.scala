package controllers.my.upload.tiling

import akka.actor.Props
import java.io.File
import controllers.my.upload.{ TaskSupervisorActor, TaskType }
import models.ContentType
import models.task.TaskService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.duration.FiniteDuration

private[tiling] class TilingSupervisorActor(
    task: TaskType,
    document: DocumentRecord,
    parts: Seq[DocumentFilepartRecord],
    documentDir: File,
    taskService: TaskService,
    keepalive: FiniteDuration
  ) extends TaskSupervisorActor(task, document, parts, documentDir, taskService, keepalive) {
  
  /** Creates workers for every image upload **/
  override def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File) =
    parts
      .filter(_.getContentType.equals(ContentType.IMAGE_UPLOAD.toString))
      .map(p => context.actorOf(Props(classOf[TilingWorkerActor], document, p, dir, taskService), name="tile.doc." + document.getId + ".part." + p.getId))
  
}
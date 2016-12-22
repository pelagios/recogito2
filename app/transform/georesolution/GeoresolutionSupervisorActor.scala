package transform.georesolution

import akka.actor.Props
import java.io.File
import models.ContentType
import models.annotation.AnnotationService
import models.task.TaskService
import models.place.PlaceService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext
import transform.TransformSupervisorActor

private[georesolution] class GeoresolutionSupervisorActor(
    document: DocumentRecord,
    parts: Seq[DocumentFilepartRecord],
    dir: File,
    taskService: TaskService,
    annotations: AnnotationService,
    places: PlaceService,
    keepalive: FiniteDuration,
    ctx: ExecutionContext
  ) extends TransformSupervisorActor(
      GeoresolutionService.TASK_TYPE,
      document,
      parts,
      dir,
      taskService,
      keepalive,
      ctx) {

  override def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File) =
    parts
      .filter(_.getContentType == ContentType.DATA_CSV.toString)
      .map(part => context.actorOf(
        Props(
          classOf[GeoresolutionWorkerActor],
          document,
          part,
          dir,
          taskService,
          annotations,
          places,
          ctx),
        name = "georesolution.doc." + document.getId + ".part." + part.getId))
  
}
package transform.georesolution

import akka.actor.Props
import java.io.File
import services.ContentType
import services.annotation.AnnotationService
import services.task.TaskService
import services.entity.EntityService
import services.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext
import transform.TransformSupervisorActor

private[georesolution] class GeoresolutionSupervisorActor(
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
      GeoresolutionService.TASK_TYPE,
      document,
      parts,
      dir,
      args,
      taskService,
      keepalive,
      ctx) {

  override def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File, args: Map[String, String]) =
    parts
      .filter(_.getContentType == ContentType.DATA_CSV.toString)
      .map(part => context.actorOf(
        Props(
          classOf[GeoresolutionWorkerActor],
          document,
          part,
          dir,
          args,
          taskService,
          annotations,
          entities,
          ctx),
        name = "georesolution.doc." + document.getId + ".part." + part.getId))
  
}
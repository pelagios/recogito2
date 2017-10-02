package transform.tei

import akka.actor.Props
import java.io.File
import models.ContentType
import models.annotation.AnnotationService
import models.task.TaskService
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import transform.TransformSupervisorActor

private[tei] class TEIParserSupervisorActor(
    document   : DocumentRecord,
    parts      : Seq[DocumentFilepartRecord],
    documentDir: File,
    annotations: AnnotationService,
    taskService: TaskService,
    keepalive  : FiniteDuration,
    ctx        : ExecutionContext
  ) extends TransformSupervisorActor(
      TEIParserService.TASK_TYPE,
      document,
      parts,
      documentDir,
      Map.empty[String, String],
      taskService,
      keepalive,
      ctx) {
  
  /** Creates workers for every TEI part **/
  override def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File, args: Map[String, String]) =
    parts
    .filter(_.getContentType.equals(ContentType.TEXT_TEIXML.toString))
    .map(part => context.actorOf(
        Props(
          classOf[TEIParserWorkerActor],
          document,
          part,
          dir,
          annotations,
          taskService,
          ctx),
        name = "tei.doc." + document.getId + ".part." + part.getId))

}
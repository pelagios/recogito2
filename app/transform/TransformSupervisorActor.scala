package transform

import akka.actor.{ Actor, ActorRef }
import akka.contrib.pattern.Aggregator
import java.io.File
import services.task.{ TaskType, TaskService }
import services.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

abstract class TransformSupervisorActor(
    taskType        : TaskType,
    document        : DocumentRecord,
    parts           : Seq[DocumentFilepartRecord],
    documentDir     : File,
    args            : Map[String, String],
    taskService     : TaskService,
    keepalive       : FiniteDuration,
    implicit val ctx: ExecutionContext
  ) extends Actor with Aggregator {
      
  import TransformTaskMessages._ 
 
  private val workers = spawnWorkers(document, parts, documentDir, args)

  private var remainingWorkers = workers.size
  
  def spawnWorkers(
    document: DocumentRecord,
    parts: Seq[DocumentFilepartRecord],
    dir: File,
    args: Map[String, String]
  ): Seq[ActorRef]

  expect {

    /** Starts child worker actors **/
    case Start =>
      if (workers.isEmpty)
        shutdown()
      else
        workers.foreach(_ ! Start)
        
    /** Supervisor actor stops once all workers have stopped **/
    case Stopped => {
      remainingWorkers -= 1
      if (remainingWorkers == 0)
        shutdown()
    }

  }
  
  private def shutdown() = {
    workers.foreach(context.stop(_))
    context.system.scheduler.scheduleOnce(keepalive) {
      taskService.deleteByTypeAndDocument(taskType, document.getId)
      context.stop(self)
    }
  }
  
}

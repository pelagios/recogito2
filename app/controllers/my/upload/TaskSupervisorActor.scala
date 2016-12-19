package controllers.my.upload

import akka.actor.{ Actor, ActorSystem, ActorRef }
import akka.contrib.pattern.Aggregator
import java.io.File
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import scala.concurrent.Future
import models.task.TaskRecordAggregate

case class TaskType(name: String) {
  
  override def toString = name
  
}

trait ProcessingService {
  
  def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord])(implicit system: ActorSystem): Unit
  
  def queryProgress(documentId: String): Future[Option[TaskRecordAggregate]]

}

private[upload] object ProcessingMessages {
  
  sealed abstract trait ProcessingMessage
  case object Start extends ProcessingMessage
  case object Stopped extends ProcessingMessage
  
}

/** A base class that encapsulates most of the functionality needed by task supervisor actors **/
abstract class TaskSupervisorActor(
    taskType: TaskType,
    document: DocumentRecord,
    parts: Seq[DocumentFilepartRecord],
    documentDir: File
  ) extends Actor with Aggregator  {
  
  import ProcessingMessages._
    
  private val workers = spawnWorkers(document, parts, documentDir)

  private var remainingWorkers = workers.size
  
  def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File): Seq[ActorRef]

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
    context.stop(self)
  }
  
}

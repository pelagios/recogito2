package controllers.my.upload

import akka.actor.{ Actor, ActorRef }
import akka.contrib.pattern.Aggregator
import java.io.File
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._
import scala.language.postfixOps

abstract class SupervisorActor(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File, keepalive: FiniteDuration) extends Actor with Aggregator  {
  
  import Messages._

  Supervisor.registerSupervisorActor(document.getId, self)
  
  private val workers = spawnWorkers(document, parts, dir)

  private var remainingWorkers = workers.size
  
  def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File): Seq[ActorRef]

  expect {

    /** Starts child worker actors **/
    case Start =>
      if (workers.isEmpty)
        shutdown(0 seconds)
      else
        workers.foreach(_ ! Start)

    /** Collects progress info from child workers and aggregates the results **/
    case QueryProgress =>
      aggregateProgressReports(document.getId, workers, sender)

    /** Once all workers are done, waits KEEPALIVE time for late-arriving progress queries then stops **/
    case Completed | Failed => {
      remainingWorkers -= 1
      if (remainingWorkers == 0)
        shutdown(keepalive)
    }

  }

  /** Sends out progress queries to child workers and collects the responses **/
  private def aggregateProgressReports(documentId: String, workers: Seq[ActorRef], origSender: ActorRef) {
    var responses = Seq.empty[WorkerProgress]
    var responseSent = false

    // After 5 seconds, we'll reply with what we have, even if not all responses are in
    context.system.scheduler.scheduleOnce(5 seconds, self, TimedOut)
    expect {
      case TimedOut => {
        if (responses.size < workers.size)
          respondIfDone(force = true)
      }
    }

    workers.foreach(w => {
      w ! QueryProgress

      expectOnce {
        case p: WorkerProgress => {
          responses = responses :+ p
          respondIfDone()
        }
      }
    })

    def respondIfDone(force: Boolean = false) =
      if (!responseSent)
        if (force || responses.size == workers.size) {
          origSender ! DocumentProgress(documentId, responses.toSeq)
          responseSent = true
        }
  }
  
  /** Waits for KEEPALIVE time, and then shuts down **/
  private def shutdown(keepalive: FiniteDuration) = {
    context.system.scheduler.scheduleOnce(keepalive) {
      Supervisor.deregisterSupervisorActor(document.getId)
      workers.foreach(context.stop(_))
      context.stop(self)
    }
  }
  
}

object Supervisor {

  // Keeps track of all currently active supervisors
  private val supervisors = scala.collection.mutable.Map.empty[String, ActorRef]

  def registerSupervisorActor(id: String, actor: ActorRef) = supervisors.put(id, actor)

  def deregisterSupervisorActor(id: String) = supervisors.remove(id)

  def getSupervisorActor(id: String) = supervisors.get(id)

}
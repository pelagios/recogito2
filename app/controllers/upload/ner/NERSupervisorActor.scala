package controllers.upload.ner

import akka.actor.{ Actor, ActorRef, Props }
import akka.contrib.pattern.Aggregator
import java.io.File
import models.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._
import scala.language.postfixOps

private[ner] class NERSupervisorActor(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File, keepalive: FiniteDuration) extends Actor with Aggregator {
    
  import NERMessages._
  
  NERSupervisor.registerActor(document.getId, self)
  
  private val workers = spawnWorkers(document, parts, dir)
  
  private var remainingWorkers = workers.size
    
  expect {
    
    /** Starts child worker actors **/
    case Start =>
      if (workers.isEmpty)
        shutdown()
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
  private def aggregateProgressReports(documentId: Int, workers: Seq[ActorRef], origSender: ActorRef) {
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
  
  /** Waits for KEEPALIVE time and then shuts down **/
  private def shutdown(keepalive: FiniteDuration = 0 seconds) = {
    context.system.scheduler.scheduleOnce(keepalive) {
      NERSupervisor.deregisterActor(document.getId)
      workers.foreach(context.stop(_))
      context.stop(self)
    }
  }
  
  /** Creates workers for ever content type indicated as 'supported' by the Worker class **/
  private def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File) =
    parts
      .filter(part => NERWorkerActor.SUPPORTED_CONTENT_TYPES.contains(part.getContentType))
      .map(p => context.actorOf(Props(classOf[NERWorkerActor], document, p, dir), name="doc_" + document.getId + "_part" + p.getId))
  
}

private[ner] object NERSupervisor {
  
  private val supervisors = scala.collection.mutable.Map.empty[Int, ActorRef]
  
  def registerActor(id: Int, actor: ActorRef) = supervisors.put(id, actor)
  
  def deregisterActor(id: Int) = supervisors.remove(id)
    
  def getActor(id: Int) = supervisors.get(id)
  
}
package controllers.myrecogito.upload.ner

import akka.actor.{ Actor, ActorRef, Props }
import akka.contrib.pattern.Aggregator
import java.io.File
import models.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Success, Failure }

private[ner] class NERSupervisorActor(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File, keepalive: Duration) extends Actor with Aggregator {
    
  import NERMessages._
  
  private val workers = spawnWorkers(document, parts, dir)
  
  private var remainingWorkers = workers.size
    
  expect {
    
    /** Spawns and starts child actors **/
    case Start => {
      if (workers.isEmpty)
        shutdown()
      else
        workers.foreach(_ ! Start)   
    }
    
    /** Collects progress info from child actors and aggregates the results **/
    case QueryProgress => {
      aggregateProgressReports(document.getId, workers, sender)      
    }
    
    /** Waits 10 more minutes for late-arriving progress queries, than stops **/
    case Completed => {
      Logger.info("[Supervisor] Workers completed")
      remainingWorkers -= 1
      if (remainingWorkers < 1)
        shutdown()
    }
    
  }
  
  private def aggregateProgressReports(documentId: Int, workers: Seq[ActorRef], origSender: ActorRef) {
    var responses = Seq.empty[WorkerProgress]
    
    // TODO timeout
    workers.foreach { w =>
      w ! QueryProgress
      expectOnce {
        case p: WorkerProgress =>
          responses = responses :+ p 
          respondIfDone()
      }
    }
    
    def respondIfDone() = {
      if (responses.size == workers.size) {
        Logger.info("[Supervisor] Sending aggregate progress response")
        origSender ! DocumentProgress(documentId, responses.toSeq)
      }      
    }
    
  }
  
  private def shutdown() = {
    // TODO clean up - we want to keep the supervisor alive for 10 more minutes
    // so we can react to late-arriving progress queries, and then stop it
    Logger.info("Shutting down")
    context.stop(self)
  }
  
  private def spawnWorkers(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File) =
    parts
      .filter(part => NERWorkerActor.SUPPORTED_CONTENT_TYPES.contains(part.getContentType))
      .map(p => context.actorOf(Props(classOf[NERWorkerActor], document, p, dir)))
  
}
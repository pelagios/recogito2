package controllers.myrecogito.upload.ner

import akka.actor.{ Actor, ActorRef, Props }
import java.io.File
import models.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import scala.concurrent.duration._

private[ner] class NERSupervisorActor(doc: DocumentRecord, parts: Seq[DocumentFilepartRecord], dir: File, keepalive: Duration = 10 minutes) extends Actor {
    
  import NERMessages._
  
  private val workers = spawnWorkers(doc, parts, dir)
  
  private var remainingWorkers = workers.size
  
  def receive = {
    
    /** Spawns and starts child actors **/
    case StartNER => {
      if (workers.isEmpty)
        shutdown()
      else
        workers.foreach(_ ! StartNER)   
    }
    
    /** Collects progress info from child actors and aggregates the results **/
    case QueryNERProgress => {
      Logger.info("Querying progress")
      // TODO delegate to child actors, aggregate the results and reply
    }
    
    /** Waits 10 more minutes for late-arriving progress queries, than stops **/
    case NERComplete => {
      remainingWorkers -= 1
      if (remainingWorkers < 1)
        shutdown()
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
      .map(p => context.actorOf(Props(classOf[NERWorkerActor], doc, p, dir)))
  
}
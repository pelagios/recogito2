package transform

import akka.actor.{ActorSystem, Props}
import java.util.UUID
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}
import storage.uploads.Uploads
import akka.routing.RoundRobinPool

class WorkerService(
  system: ActorSystem,
  uploads: Uploads,
  actorProps: Props,
  workerInstances: Int
) {
  
  val routerProps = actorProps
    .withRouter(RoundRobinPool(nrOfInstances = workerInstances))
    .withDispatcher("contexts.background-workers")
  
  val router = system.actorOf(routerProps)
  
  def spawnJob(
    document: DocumentRecord,
    parts   : Seq[DocumentFilepartRecord],
    args    : Map[String, String] = Map.empty[String, String]
  ) = parts.foreach { part =>  
    val jobId = UUID.randomUUID

    router ! WorkerActor.WorkOnPart(
      jobId,
      document,
      part,
      uploads.getDocumentDir(document.getOwner, document.getId).get,
      args)

    jobId
  }
  
}
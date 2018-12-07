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
  
  /** Spawns a new job on the given document & parts **/
  private def spawn(
    document: DocumentRecord,
    parts   : Seq[DocumentFilepartRecord],
    jobDef  : Option[SpecificJobDefinition]
  ) = {
    // Create a job ID
    val jobId = UUID.randomUUID

    // Distribute tasks to workers
    parts.foreach { part =>  
      router ! WorkerActor.WorkOnPart(
        jobId,
        document,
        part,
        uploads.getDocumentDir(document.getOwner, document.getId).get,
        jobDef)
    }

    // Return job ID for outside reference
    jobId
  }

  /** Spawns a job with a specific job defintion **/
  def spawnJob(
    document: DocumentRecord, 
    parts: Seq[DocumentFilepartRecord], 
    jobDef: SpecificJobDefinition) = spawn(document, parts, Some(jobDef))

  /** Spawns a job that doesn't need a specific job definition **/
  def spawnJob(
    document: DocumentRecord, 
    parts: Seq[DocumentFilepartRecord]) = spawn(document, parts, None)

}
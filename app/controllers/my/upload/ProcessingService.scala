package controllers.my.upload

import akka.actor.ActorSystem
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }

/** The only common traits of all processing services: they can spawn a task and report progress **/
trait ProcessingService {
  
  import ProcessingTaskMessages._
  
  def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord])(implicit system: ActorSystem): Unit
  
  def queryProgress(documentId: String, timeout: FiniteDuration = 10 seconds)(implicit context: ExecutionContext, system: ActorSystem): Future[Option[DocumentProgress]]
  
}

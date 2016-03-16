package controllers.my.upload.tiling

import akka.actor.{ ActorSystem, Props }
import akka.pattern.ask
import akka.util.Timeout
import controllers.my.upload.{ Messages, Supervisor }
import java.io.File
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import storage.FileAccess
import sys.process._

object TilingService extends FileAccess {
  
  private[tiling] def createZoomify(file: File, destFolder: File): Future[Unit] = {
    Future {
      s"vips dzsave $file $destFolder --layout zoomify" !
    } map { result =>
      if (result == 0)
        Unit
      else
        throw new Exception("Image tiling failed for " + file.getAbsolutePath + " to " + destFolder.getAbsolutePath)
    }
  }
  
  /** Spawns a new background tiling process.
    *
    * The function will throw an exception in case the user data directory
    * for any of the fileparts does not exist. This should, however, never
    * happen. If it does, something is seriously broken with the DB integrity.
    */
  def spawnTilingProcess(document: DocumentRecord, parts: Seq[DocumentFilepartRecord])(implicit system: ActorSystem): Unit =
    spawnTilingProcess(document, parts, getUserDir(document.getOwner).get)
    
  /** We're splitting this function, so we can inject alternative folders for testing **/
  private[tiling] def spawnTilingProcess(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], sourceFolder: File, keepalive: Duration = 10 minutes)(implicit system: ActorSystem): Unit = {
    Logger.info("Starting image tiling process")
    val actor = system.actorOf(Props(classOf[TilingSupervisorActor], document, parts, sourceFolder, keepalive), name = "doc_" + document.getId)
    actor ! Messages.Start
  }
  
  /** Queries the progress for a specific process **/
  def queryProgress(documentId: String, timeout: FiniteDuration = 10 seconds)(implicit system: ActorSystem) = {
    Logger.info("Reporting tiling progress")
    Supervisor.getSupervisorActor(documentId) match {
      case Some(actor) => {
        implicit val t = Timeout(timeout)
        (actor ? Messages.QueryProgress).mapTo[Messages.DocumentProgress].map(Some(_))
      }

      case None =>
        Future.successful(None)
    }
  }
  
}
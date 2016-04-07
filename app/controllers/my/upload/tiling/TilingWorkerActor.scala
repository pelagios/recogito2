package controllers.my.upload.tiling

import akka.actor.Actor
import controllers.my.upload.ProgressStatus
import java.io.File
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

class TilingWorkerActor(document: DocumentRecord, part: DocumentFilepartRecord, documentDir: File) extends Actor{

  import controllers.my.upload.ProcessingTaskMessages._

  var progress = 0.0
  var status = ProgressStatus.PENDING

  def receive = {

    case Start => {
      status = ProgressStatus.IN_PROGRESS
      
      val origSender = sender
      val filename = part.getFilename
      val tilesetDir= new File(documentDir, filename.substring(0, filename.lastIndexOf('.')))
      
      TilingService
        .createZoomify(new File(documentDir, filename), tilesetDir).map(_ => {
          progress = 1.0
          status = ProgressStatus.COMPLETED
          origSender ! Completed
        }).recover { case t =>  {
          t.printStackTrace
          status = ProgressStatus.FAILED
          origSender ! Failed(t.getMessage)
        }}
    }

    case QueryProgress =>
      sender ! WorkerProgress(part.getId, status, progress)

  }
  
}
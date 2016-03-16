package controllers.my.upload.tiling

import akka.actor.Actor
import java.io.File
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }

class TilingWorkerActor(document: DocumentRecord, part: DocumentFilepartRecord, dir: File) extends Actor{

  import controllers.my.upload.Messages._

  var progress = 0.0

  def receive = {

    case Start => {
      val origSender = sender
      /*
      parseFilepart(document, part, dir).map { result =>
        val annotations = phrasesToAnnotations(result, document, part)
        AnnotationService.insertAnnotations(annotations)
        progress = 1.0
        origSender ! Completed
      }.recover { case t => {
        t.printStackTrace
        origSender ! Failed(t.getMessage)
      }}
      */
    }

    case QueryProgress =>
      sender ! WorkerProgress(part.getId, progress)

  }
  
}
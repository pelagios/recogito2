package controllers.myrecogito.upload.ner

import akka.actor.Actor
import java.io.File
import models.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.io.Source

private[ner] class NERWorkerActor(document: DocumentRecord, part: DocumentFilepartRecord, dir: File) extends Actor {
  
  import NERMessages._
  
  var progress = 0.0
  
  def receive = {
    
    case Start => Future {
      val result = parseFilepart(document, part, dir)
      Logger.info("Done.")
      result.foreach(phrase => Logger.info(phrase.toString))
      
      // TODO convert result to annotations
      
      // TODO import to DB
      
      progress = 1.0
      
      sender ! Completed
      context.stop(self) 
    }
    
    case QueryProgress => 
      sender ! WorkerProgress(part.getId, progress)
      
  }

  /** Select appropriate parser for part content type **/
  private def parseFilepart(document: DocumentRecord, part: DocumentFilepartRecord, dir: File) =
    part.getContentType match {
      case t if t == ContentTypes.TEXT_PLAIN.toString =>
        parsePlaintext(document, part, new File(dir, part.getFilename))
          
      case t => {
        Logger.info("Skipping NER for file of unsupported type " + t + ": " + dir.getName + File.separator + part.getFilename) 
        Seq.empty[Phrase]
      }
    }
  
  private def parsePlaintext(document: DocumentRecord, part: DocumentFilepartRecord, file: File) = {
    val text = Source.fromFile(file).getLines.mkString("\n")
    NERService.parse(text)
  }
  
}

private[ner] object NERWorkerActor {
  
  val SUPPORTED_CONTENT_TYPES = Set(ContentTypes.TEXT_PLAIN).map(_.toString)
  
}

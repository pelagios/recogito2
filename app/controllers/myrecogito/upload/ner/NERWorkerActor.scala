package controllers.myrecogito.upload.ner

import akka.actor.Actor
import java.io.File
import models.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import scala.io.Source

private[ner] class NERWorkerActor(document: DocumentRecord, part: DocumentFilepartRecord, dir: File) extends Actor {
  
  import NERMessages._
  
  def receive = {
    
    case StartNER => {
      val result = parseFilepart(document, part, dir)
      
      // TODO convert result to annotations
      
      // TODO import to DB
      
      sender ! NERComplete
      context.stop(self) 
    }
      
  }

  /** Select appropriate parser for part content type **/
  private def parseFilepart(document: DocumentRecord, part: DocumentFilepartRecord, dir: File) =
    part.getContentType match {
      case t if t == ContentTypes.TEXT_PLAIN.toString =>
        parsePlaintext(document, part, new File(dir, part.getFilename))
          
      case t =>
        Logger.info("Skipping NER for file of unsupported type " + t + ": " + dir.getName + File.separator + part.getFilename) 
    }
  
  private def parsePlaintext(document: DocumentRecord, part: DocumentFilepartRecord, file: File) = {
    val text = Source.fromFile(file).getLines.mkString("\n")
    NERService.parse(text)
  }
  
}

private[ner] object NERWorkerActor {
  
  val SUPPORTED_CONTENT_TYPES = Set(ContentTypes.TEXT_PLAIN).map(_.toString)
  
}

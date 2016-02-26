package controllers.myrecogito.upload.ner

import akka.actor.Actor
import java.io.File
import models.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import scala.io.Source

private[ner] class NERActor(document: DocumentRecord, parts: Seq[(DocumentFilepartRecord, File)]) extends Actor {
  
  Logger.info("yay")
  
  import NERActor._
  
  def receive = {
    
    case StartNER => {
      Logger.info("Starting")
      parseFileparts(document, parts)
      Logger.info("NER completed")
      sender ! NERComplete
    }
      
  }

  private def parseFileparts(document: DocumentRecord, parts: Seq[(DocumentFilepartRecord, File)]) = {
    parts.map { tuple => {
      Logger.info(tuple._2.getName)
    }}

        
        
        /*_ match {
      case (part, file) if part.getContentType == ContentTypes.TEXT_PLAIN.toString => 
        parsePlaintext(document, part, file)
        
      case (part, file) =>
        Logger.info("Skipping NER for file of unsupported type " + part.getContentType + ": " + file.getAbsolutePath) 
    })*/
  }
  
  private def parsePlaintext(document: DocumentRecord, part: DocumentFilepartRecord, file: File) = {
    Logger.info("Staring NER")
    
    val text = Source.fromFile(file).getLines.mkString("\n")
    val entities = NERService.parse(text)
    
    // TODO implement
  }
  
}

object NERActor {

  sealed abstract trait Message
  
  case object StartNER extends Message
  case object QueryNERProgress extends Message
  case class NERProgress(value: Double, currentPhase: String) extends Message
  case object NERComplete extends Message

}

package controllers.myrecogito.upload.ner

import akka.actor.Actor
import java.io.File
import models.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
import scala.io.Source

private[ner] case class PartWithFile(part: DocumentFilepartRecord, file: File)

private[ner] class NERActor(document: DocumentRecord, parts: Seq[PartWithFile]) extends Actor {
  
  import NERActor._
  
  def receive = {
    
    case StartNER => {
      val result = parseFileparts(document, parts)
      sender ! NERComplete(result)
    }
      
  }

  private def parseFileparts(document: DocumentRecord, parts: Seq[PartWithFile]): Map[Int, Seq[Phrase]] = {
    parts.map(partWithFile => {
      partWithFile match {
        case p if p.part.getContentType == ContentTypes.TEXT_PLAIN.toString =>
          (p.part.getId.toInt, parsePlaintext(document, p.part, p.file))
          
        case p => {
          Logger.info("Skipping NER for file of unsupported type " + p.part.getContentType + ": " + p.file.getAbsolutePath) 
          (p.part.getId.toInt, Seq.empty[Phrase])          
        }
      }
    }).toMap    
  }
  
  private def parsePlaintext(document: DocumentRecord, part: DocumentFilepartRecord, file: File) = {
    val text = Source.fromFile(file).getLines.mkString("\n")
    NERService.parse(text)
  }
  
}

object NERActor {

  sealed abstract trait Message
  
  case object StartNER extends Message
  case object QueryNERProgress extends Message
  case class NERProgress(value: Double, currentPhase: String) extends Message
  case class NERComplete(result: Map[Int, Seq[Phrase]]) extends Message

}

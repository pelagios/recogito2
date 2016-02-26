package controllers.myrecogito.upload.ner

import akka.actor.Actor
import java.io.File
import models.ContentTypes
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import play.api.Logger
// import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.io.Source
import storage.FileAccess

private[ner] class NERActor(document: DocumentRecord, fileparts: Seq[DocumentFilepartRecord]) extends Actor with FileAccess {
  
  import NERActor._
  
  def receive = {
    
    case StartNER => // TODO implement
      
  }

  private def parseFileparts() = {
    // This will throw an exception if user data dir does not exist - can only
    // happen in case DB integrity is broken!
    val userDir = getUserDir(document.getOwner).get
    
    val files = fileparts.map(part => (part, new File(userDir, part.getFilename)))
    
    // Process files in parallel
    files.par.map(_ match {
      case (part, file) if part.getContentType == ContentTypes.TEXT_PLAIN => 
        parsePlaintext(document, part, file)
        
      case (part, file) =>
        Logger.info("Skipping NER for file of unsupported type " + part.getContentType + ": " + file.getAbsolutePath) 
    })
  }
  
  private def parsePlaintext(document: DocumentRecord, part: DocumentFilepartRecord, file: File) = {
    val text = Source.fromFile(file).getLines.mkString("\n")
    
    // TODO implement
    
  }
  
}

object NERActor {

  sealed abstract trait Message
  
  case object StartNER extends Message
  case object QueryNERProgress extends Message
  case class NERProgress(value: Double, currentPhase: String) extends Message

}

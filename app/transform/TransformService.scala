package transform

import akka.actor.ActorSystem 
import services.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }

private[transform] object TransformTaskMessages {
  
  sealed abstract trait TransformTaskMessage
  
  case object Start extends TransformTaskMessage
  
  case object Stopped extends TransformTaskMessage
  
}

trait TransformService {
    
  def spawnTask(document: DocumentRecord, parts: Seq[DocumentFilepartRecord], args: Map[String, String] = Map.empty[String, String])(implicit system: ActorSystem): Unit

}


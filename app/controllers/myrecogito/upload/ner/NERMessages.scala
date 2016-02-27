package controllers.myrecogito.upload.ner

object NERMessages{

  sealed abstract trait Message
  
  case object StartNER extends Message
  
  case object QueryNERProgress extends Message

  case class NERProgress(value: Double, currentPhase: String) extends Message

  case object NERComplete extends Message

}
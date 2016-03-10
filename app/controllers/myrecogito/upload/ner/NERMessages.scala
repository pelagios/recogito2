package controllers.myrecogito.upload.ner

object NERMessages{

  sealed abstract trait Message

  case object Start extends Message

  case object QueryProgress extends Message

  case class WorkerProgress(filepartId: Int, progress: Double)

  case class DocumentProgress(documentId: Int, progress: Seq[WorkerProgress]) extends Message

  case object TimedOut extends Message

  case class Failed(msg: String) extends Message

  case object Completed extends Message

}

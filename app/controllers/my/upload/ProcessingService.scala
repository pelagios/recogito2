package controllers.my.upload

import akka.actor.{ ActorRef, ActorSystem }
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps

case class TaskType(name: String)

trait ProcessingService {
  
  def queryProgress(documentId: String, timeout: FiniteDuration = 10 seconds)(implicit system: ActorSystem): Future[Option[Messages.DocumentProgress]]
  
}

object Supervisor {

  // Keeps track of all currently active supervisors
  private val supervisors = scala.collection.mutable.Map.empty[(TaskType, String), ActorRef]

  def registerSupervisorActor(task: TaskType, id: String, actor: ActorRef) = supervisors.put((task, id), actor)

  def deregisterSupervisorActor(task: TaskType, id: String) = supervisors.remove((task, id))

  def getSupervisorActor(task: TaskType, id: String) = supervisors.get((task, id))

}
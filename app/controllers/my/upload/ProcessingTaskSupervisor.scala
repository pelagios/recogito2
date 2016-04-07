package controllers.my.upload

import akka.actor.ActorRef

/** Helper class to keep track of all currently active task supervisor actors.
  *
  * Reminder: one supervisor actor is responsible for ONE task currently being
  * performed on ONE document. 
  */
object ProcessingTaskSupervisor {
  
  private val supervisors = scala.collection.mutable.Map.empty[(TaskType, String), ActorRef]

  def registerSupervisorActor(task: TaskType, id: String, actor: ActorRef) = supervisors.put((task, id), actor)

  def deregisterSupervisorActor(task: TaskType, id: String) = supervisors.remove((task, id))

  def getSupervisorActor(task: TaskType, id: String) = supervisors.get((task, id))
  
}
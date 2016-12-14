package models.task

import java.sql.Timestamp
import storage.DB
import javax.inject.{ Inject, Singleton }

private[task] case class ProgressTracker(

  private val db: DB,
  
  val taskType: String,
  
  val className: String,
  
  val lookupKey: Option[String],
  
  val spawnedBy: Option[String],
  
  val spawnedAt: Timestamp,
  
  val stoppedAt: Option[Timestamp],
  
  val stoppedWith: Option[String],
  
  val status: String,
  
  val progress: Int
  
) {
  
  def updateStatus(status: String) = {
    // TODO persist to DB
    this.copy(status = status)
  }
  
  def updateProgress(progress: Int) = {
    // TODO persist to DB
    this.copy(progress = progress)
  }
  
}

@Singleton
class TaskService @Inject() (val db: DB) {

  def newProgressTracker(
    taskType: String,
    className: String,
    lookupKey: Option[String],
    spawnedBy: Option[String]
  ) = {
    
    // TODO persist to DB
    
    ProgressTracker(
      db,
      taskType,
      className,
      lookupKey,
      spawnedBy,
      new Timestamp(System.currentTimeMillis),
      None,
      None,
      "PENDING", // TODO enum
      0)
  }

}

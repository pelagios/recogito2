package models.task

import javax.inject.{ Inject, Singleton }
import java.sql.Timestamp
import java.util.UUID
import models.generated.Tables.TASK
import scala.concurrent.{ ExecutionContext, Future }
import storage.DB

private[task] case class ProgressTracker(

  private val db: DB,
  
  private implicit val ctx: ExecutionContext,
  
  val id: UUID,
  
  val taskType: String,
  
  val className: String,
  
  val lookupKey: Option[String],
  
  val spawnedBy: Option[String],
  
  val spawnedAt: Timestamp,
  
  val stoppedAt: Option[Timestamp],
  
  val stoppedWith: Option[String],
  
  val status: TaskStatus.Value,
  
  val progress: Int
  
) {
  
  def commit(): Future[ProgressTracker] = {
    // TODO persist to DB
    null
  }
  
  def updateStatus(status: TaskStatus.Value): Future[ProgressTracker] = {
    val updated = this.copy(status = status)
    updated.commit()
  }
  
  def updateProgress(progress: Int) = {
    val updated = this.copy(progress = progress)
    updated.commit()
  }
  
}

object TaskStatus extends Enumeration {

  val PENDING = Value("PENDING")

  val RUNNING = Value("RUNNING")
  
  val COMPLETED = Value("COMPLETED")
  
  val FAILED = Value("FAILED")

}

@Singleton
class TaskService @Inject() (val db: DB, implicit val ctx: ExecutionContext) {
  
  def newProgressTracker(
    taskType: String,
    className: String,
    lookupKey: Option[String],
    spawnedBy: Option[String]
  ): Future[ProgressTracker] = {
    
    val tracker =
      ProgressTracker(
        db,
        ctx,
        UUID.randomUUID,
        taskType,
        className,
        lookupKey,
        spawnedBy,
        new Timestamp(System.currentTimeMillis),
        None,
        None,
        TaskStatus.PENDING,
        0)
     
    tracker.commit().map(_ => tracker)     
  }

}

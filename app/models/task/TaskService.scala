package models.task

import javax.inject.{ Inject, Singleton }
import java.sql.Timestamp
import java.util.UUID
import models.BaseService
import models.generated.Tables.TASK
import models.generated.tables.records.TaskRecord
import scala.concurrent.{ ExecutionContext, Future }
import storage.DB

object TaskStatus extends Enumeration {

  val PENDING = Value("PENDING")

  val RUNNING = Value("RUNNING")
  
  val COMPLETED = Value("COMPLETED")
  
  val FAILED = Value("FAILED")

}

@Singleton
class TaskService @Inject() (val db: DB, implicit val ctx: ExecutionContext) extends BaseService {
  
  def findById(uuid: UUID) = db.query { sql =>
    sql.selectFrom(TASK).where(TASK.ID.equal(uuid)).fetchOne()
  }
  
  def findByTypeAndKey(taskType: String, lookupKey: String) = db.query { sql =>
    sql.selectFrom(TASK).where(TASK.TASK_TYPE.equal(taskType)).and(TASK.LOOKUP_KEY.equal(lookupKey)).fetchOne()    
  }
  
  def updateProgress(uuid: UUID, progress: Int): Future[Unit] = db.withTransaction { sql => 
    sql.update(TASK)
      .set[Integer](TASK.PROGRESS, progress)
      .where(TASK.ID.equal(uuid))
      .execute()
  }
    
  def updateStatus(uuid: UUID, status: TaskStatus.Value): Future[Unit] = db.withTransaction { sql => 
    sql.update(TASK)
      .set(TASK.STATUS, status.toString)
      .where(TASK.ID.equal(uuid))
      .execute()
  }
  
  def insertTask(taskType: String, className: String, lookupKey: Option[String], spawnedBy: Option[String]): Future[UUID] = db.withTransaction { sql =>
    val uuid = UUID.randomUUID
    
    val taskRecord = new TaskRecord(
      uuid,
      taskType,
      className,
      optString(lookupKey),
      optString(spawnedBy),
      new Timestamp(System.currentTimeMillis),
      null, // stopped_at
      null, // stopped_with
      TaskStatus.PENDING.toString,
      0
    )
    
    sql.insertInto(TASK).set(taskRecord).execute()
    
    uuid
  }

}

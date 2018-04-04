package services.announcement

import java.sql.Timestamp
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import services.BaseService
import services.generated.Tables.SERVICE_ANNOUNCEMENT
import services.generated.tables.records.ServiceAnnouncementRecord
import storage.db.DB
import services.user.UserService
import java.util.UUID

@Singleton
class AnnouncementService @Inject() (val db: DB, users: UserService, implicit val ctx: ExecutionContext) extends BaseService {
  
  def findLatestUnread(username: String): Future[Option[ServiceAnnouncementRecord]] = db.query { sql =>
    Option(sql.selectFrom(SERVICE_ANNOUNCEMENT)
              .where(SERVICE_ANNOUNCEMENT.FOR_USER.equal(username)
                .and(SERVICE_ANNOUNCEMENT.RESPONSE.isNull))
              .orderBy(SERVICE_ANNOUNCEMENT.CREATED_AT.desc())
              .fetchOne())
  }
  
  def confirm(uuid: UUID, username: String, response: String): Future[Boolean] = db.query { sql =>
    val result =
      sql.update(SERVICE_ANNOUNCEMENT)
         .set(SERVICE_ANNOUNCEMENT.VIEWED_AT, new Timestamp(new Date().getTime))
         .set(SERVICE_ANNOUNCEMENT.RESPONSE, response)
         .where(SERVICE_ANNOUNCEMENT.ID.equal(uuid).and(SERVICE_ANNOUNCEMENT.FOR_USER.equal(username)))
         .execute()
         
    result == 1
  }
  
  def clearAll(): Future[Boolean] = db.query { sql =>
    val result = sql.deleteFrom(SERVICE_ANNOUNCEMENT).execute()
    true
  } recover { case t: Throwable =>
    t.printStackTrace()
    false
  }
  
  def insertBroadcastAnnouncement(content: String): Future[Boolean] =  {
    val BATCH_SIZE = 200
    
    def insertOneBatch(users: Seq[String]): Future[_] = db.query { sql =>
      sql.batch(users.map { user =>
        sql.insertInto(SERVICE_ANNOUNCEMENT,
          SERVICE_ANNOUNCEMENT.ID, 
          SERVICE_ANNOUNCEMENT.FOR_USER,
          SERVICE_ANNOUNCEMENT.CONTENT,
          SERVICE_ANNOUNCEMENT.CREATED_AT,
          SERVICE_ANNOUNCEMENT.VIEWED_AT,
          SERVICE_ANNOUNCEMENT.RESPONSE
        ).values(
          UUID.randomUUID(),
          user,
          content,
          new Timestamp(new Date().getTime),
          null,
          null)
      }:_*).execute()
    }
    
    def insertBatchesRecursive(offset: Int, numUsers: Int): Future[Boolean] =
      users.listUsers(offset, BATCH_SIZE, None, None).flatMap { users =>
        insertOneBatch(users.items.map(_.getUsername))
      } flatMap { _ =>
        if (offset + BATCH_SIZE >= numUsers)
          Future.successful(true)
        else
          insertBatchesRecursive(offset + BATCH_SIZE, numUsers)
      }
      
    val f = for {
      numUsers <- users.countUsers()
      success <- insertBatchesRecursive(0, numUsers)
    } yield (success)
    
    f.recover { case t: Throwable =>
      play.api.Logger.info(t.getMessage)
      t.printStackTrace()
      false
    }
  }
  
}
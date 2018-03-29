package services.announcement

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import services.BaseService
import storage.db.DB
import services.generated.Tables.SERVICE_ANNOUNCEMENT
import services.generated.tables.records.ServiceAnnouncementRecord

@Singleton
class AnnouncementService @Inject() (val db: DB, implicit val ctx: ExecutionContext) extends BaseService {
  
  def findForUser(username: String): Future[Option[ServiceAnnouncementRecord]] = db.query { sql =>
    Option(sql.selectFrom(SERVICE_ANNOUNCEMENT).where(SERVICE_ANNOUNCEMENT.FOR_USER.equal(username)).fetchOne())
  }
  
}
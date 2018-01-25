package services.entity.builtin.importer;

import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import services.entity.EntityRecord

trait HasBatchImport { 

  def importRecord(record: EntityRecord): Future[Boolean]
  
  // Maximum number of times a gazetteer record or place link update will be retried in case of failure
  private def MAX_RETRIES = 20
  
  // Incremental backoff time (ms) for index retries
  private val BACKOFF_MS = 500
  
  /** Imports a batch of entity records into the index
    * 
    * @return the list of records that failed to import (ideally, an empty list)
    */
  def importRecords(records: Seq[EntityRecord], retries: Int = MAX_RETRIES)(implicit ctx: ExecutionContext): Future[Seq[EntityRecord]] =
    records.foldLeft(Future.successful(Seq.empty[EntityRecord])) { case (f, next) =>
      f.flatMap { failed =>
        importRecord(next).map { success =>
          if (success) failed
          else next +: failed
        }
      }
    } flatMap { failedRecords =>      
      if (failedRecords.size > 0 && retries > 0) {
        Logger.warn(s"${failedRecords.size} gazetteer records failed to import - retrying")

        // Start first retry immediately and then increases wait time for each subsequent retry
        val backoff = (MAX_RETRIES - retries) * BACKOFF_MS
        if (backoff > 0) {
          Logger.info(s"Waiting... ${backoff}ms")
          Thread.sleep(backoff)
        }

        importRecords(failedRecords, retries - 1)
      } else if (failedRecords.size > 0) {
        Logger.error(s"${failedRecords.size} gazetteer records failed without recovery")
        failedRecords.foreach(record =>  Logger.error(record.toString))
        Future.successful(failedRecords)
      } else {
        Logger.info(s"Imported ${records.size} records - no failed imports")
        Future.successful(failedRecords)
      }
    }
  
}
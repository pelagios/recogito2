package models.content

import collection.JavaConversions._
import models.BaseService
import models.generated.Tables._
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord }
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.Record
import play.api.Logger
import play.api.cache.CacheApi
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import storage.{ DB, FileAccess }
import org.apache.commons.io.FileUtils

object DocumentService extends BaseService with FileAccess {
  
  // We use random alphanumeric IDs with 14 chars length (because 62^14 should be enough for anyone (TM))  
  private val ID_LENGTH = 14
  
  private[content] def generateRandomID(retries: Int = 10)(implicit db: DB): String = {
    // Generate 10 random IDs
    val randomIds = 
      (1 to 10).map(_ => RandomStringUtils.randomAlphanumeric(ID_LENGTH).toLowerCase).toSet

    // Match them all against the database and remove those that already exist
    val idsAlreadyInDB = Await.result(findIDs(randomIds), 10 seconds)    
    val uniqueIds = randomIds.filter(id => !idsAlreadyInDB.contains(id))
    
    if (uniqueIds.size > 0) {
      uniqueIds.head
    } else if (retries > 0) {
      Logger.warn("Failed to generate unique random document ID")
      generateRandomID(retries - 1)
    } else {
      throw new RuntimeException("Failed to create unique document ID")
    }
  }
  
  private def findIDs(ids: Set[String])(implicit db: DB) = db.query { sql =>
    sql.select(DOCUMENT.ID)
       .from(DOCUMENT)
       .where(DOCUMENT.ID.in(ids))
       .fetchArray()
       .map(_.value1).toSet    
  }
  
  def findById(id: String)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(DOCUMENT).where(DOCUMENT.ID.equal(id)).fetchOne())
  }
  
  def delete(document: DocumentRecord)(implicit db: DB) = db.withTransaction { sql =>
    sql.deleteFrom(DOCUMENT_FILEPART)
       .where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(document.getId))
       .execute()

    // Some document may not have local files - e.g. IIIF  
    val maybeDocumentDir = getDocumentDir(document.getOwner, document.getId)
    if (maybeDocumentDir.isDefined)
      FileUtils.deleteDirectory(maybeDocumentDir.get)
    
    sql.deleteFrom(DOCUMENT)
       .where(DOCUMENT.ID.equal(document.getId))
       .execute()
  }

  def findByUser(username: String, offset: Int = 0, limit: Int = 20)(implicit db: DB) = db.query { sql =>
    sql.selectFrom(DOCUMENT)
       .where(DOCUMENT.OWNER.equal(username))
       .limit(limit)
       .offset(offset)
       .fetchArray().toSeq
  }

  /** This method is cached as it gets accessed a lot **/
  def findByIdWithFileparts(id: String)(implicit db: DB, cache: CacheApi) =
    cachedLookup("doc", id, findByIdWithFilepartsNoCache)
  
  private def findByIdWithFilepartsNoCache(id: String)(implicit db: DB) = db.query { sql =>
    val records =
      sql.selectFrom(DOCUMENT
        .join(DOCUMENT_FILEPART)
        .on(DOCUMENT.ID.equal(DOCUMENT_FILEPART.DOCUMENT_ID)))
      .where(DOCUMENT.ID.equal(id))
      .fetchArray()

    // Convert to (DocumentRecord, Seq[DocumentFilepartRecord) tuple
    val grouped = groupJoinResult(records, classOf[DocumentRecord], classOf[DocumentFilepartRecord])
    if (grouped.size > 1)
      throw new RuntimeException("Got " + grouped.size + " DocumentRecords with the same ID: " + grouped.keys.map(_.getId).mkString(", "))

    grouped.headOption
  }

  def findPartByDocAndSeqNo(docId: String, seqNo: Int)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(DOCUMENT_FILEPART)
              .where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(docId))
              .and(DOCUMENT_FILEPART.SEQUENCE_NO.equal(seqNo))
              .fetchOne())
  }

}

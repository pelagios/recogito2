package services.document

import collection.JavaConversions._
import org.apache.commons.lang3.RandomStringUtils
import play.api.Logger
import scala.concurrent.Await
import scala.concurrent.duration._
import services.generated.Tables._
import storage.db.DB

trait DocumentIdFactory { self: DocumentService =>
  
  // We use random alphanumeric IDs with 14 chars length (because 62^14 should be enough for anyone (TM))  
  private val ID_LENGTH = 14
  
  // Utility function to check if an ID exists in the DB
  def existsId(id: String) = {
    def checkExists() = db.query { sql =>
      val count = sql.select(DOCUMENT.ID)
         .from(DOCUMENT)
         .where(DOCUMENT.ID.equal(id))
         .fetchArray()
         .length
      
      count > 0
    }
    
    Await.result(checkExists(), 10.seconds)    
  }
  
  def generateRandomID(retriesLeft: Int = 10): String = {
    
    // Takes a set of strings and returns those that already exist in the DB as doc IDs
    def findIds(ids: Set[String])(implicit db: DB) = db.query { sql =>
      sql.select(DOCUMENT.ID)
         .from(DOCUMENT)
         .where(DOCUMENT.ID.in(ids))
         .fetchArray()
         .map(_.value1).toSet    
    }
    
    // Generate 10 random IDs
    val randomIds = 
      (1 to 10).map(_ => RandomStringUtils.randomAlphanumeric(ID_LENGTH).toLowerCase).toSet

    // Match them all against the database and remove those that already exist
    val idsAlreadyInDB = Await.result(findIds(randomIds), 10.seconds)    
    val uniqueIds = randomIds.filter(id => !idsAlreadyInDB.contains(id))
    
    if (uniqueIds.size > 0) {
      uniqueIds.head
    } else if (retriesLeft > 0) {
      Logger.warn("Failed to generate unique random document ID")
      generateRandomID(retriesLeft - 1)
    } else {
      throw new RuntimeException("Failed to create unique document ID")
    }
  }
  
}
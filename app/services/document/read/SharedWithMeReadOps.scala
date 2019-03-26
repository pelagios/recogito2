package services.document.read

import collection.JavaConversions._
import services.document.DocumentService
import services.generated.Tables.SHARING_POLICY

trait SharedWithMeReadOps { self: DocumentService =>

  /** Returns the number of documents shared with a given user **/
  def countDocumentsSharedWithMe(sharedWith: String) = db.query { sql =>
    sql.selectCount()
       .from(SHARING_POLICY)
       .where(SHARING_POLICY.SHARED_WITH.equal(sharedWith))
       .fetchOne(0, classOf[Int])
  }
  
  /** Convenience method to list all document IDs shared with the given user **/
  def listAllIdsSharedWithMe(username: String) = db.query { sql => 
    sql.select(SHARING_POLICY.DOCUMENT_ID)
       .from(SHARING_POLICY)
       .where(SHARING_POLICY.SHARED_WITH.equal(username))
       .fetch(0, classOf[String]).toSeq
  }

}
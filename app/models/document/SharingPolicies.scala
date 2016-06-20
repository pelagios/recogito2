package models.document

import models.generated.Tables._
import storage.DB

trait SharingPolicies {
  
  def listDocumentCollaborators(documentId: String)(implicit db: DB) = db.query { sql =>
    sql.selectFrom(SHARING_POLICY).where(SHARING_POLICY.DOCUMENT_ID.equal(documentId)).fetchArray().toSeq
  }
  
  def countSharedDocuments(username: String)(implicit db: DB) = db.query { sql =>
    sql.selectCount().from(SHARING_POLICY).where(SHARING_POLICY.SHARED_WITH.equal(username)).fetchOne(0, classOf[Int])
  }
  
  def listSharedDocuments(username: String, offset: Int = 0, limit: Int = 20)(implicit db: DB) = db.query { sql =>
    
  }
  
}
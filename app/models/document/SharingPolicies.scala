package models.document

import java.sql.Timestamp
import java.util.Date
import models.generated.Tables._
import models.generated.tables.records.SharingPolicyRecord
import storage.DB

trait SharingPolicies {
  
  def addDocumentCollaborator(documentId: String, sharedBy: String, sharedWith: String, accessLevel: DocumentAccessLevel)(implicit db: DB) = db.query { sql =>
    val sharingPolicy = new SharingPolicyRecord(null, null,
      documentId, 
      sharedBy,
      sharedWith,
      new Timestamp(new Date().getTime),
      accessLevel.toString)
    
    sql.insertInto(SHARING_POLICY).set(sharingPolicy).execute() == 1
  } 
  
  def removeDocumentCollaborator(documentId: String, sharedWith: String)(implicit db: DB) = db.query { sql =>
    sql.deleteFrom(SHARING_POLICY)
       .where(SHARING_POLICY.DOCUMENT_ID.equal(documentId)
         .and(SHARING_POLICY.SHARED_WITH.equal(sharedWith)))
       .execute() == 1
  } 
  
  def listDocumentCollaborators(documentId: String)(implicit db: DB) = db.query { sql =>
    sql.selectFrom(SHARING_POLICY).where(SHARING_POLICY.DOCUMENT_ID.equal(documentId)).fetchArray().toSeq
  }
  
  def countBySharedWith(sharedWith: String)(implicit db: DB) = db.query { sql =>
    sql.selectCount().from(SHARING_POLICY).where(SHARING_POLICY.SHARED_WITH.equal(sharedWith)).fetchOne(0, classOf[Int])
  }
  
  def findBySharedWith(sharedWith: String, offset: Int = 0, limit: Int = 20)(implicit db: DB) = db.query { sql =>
    
  }
  
}
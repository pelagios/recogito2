package models.document

import java.sql.Timestamp
import java.util.Date
import models.Page
import models.generated.Tables._
import models.generated.tables.records.{ DocumentRecord, SharingPolicyRecord }
import storage.DB

trait SharingPolicies {
  
  def addDocumentCollaborator(documentId: String, sharedBy: String, sharedWith: String, accessLevel: DocumentAccessLevel)(implicit db: DB) = db.query { sql =>
    val sharingPolicy = new SharingPolicyRecord(null, null,
      documentId, 
      sharedBy,
      sharedWith,
      new Timestamp(new Date().getTime),
      accessLevel.toString)
    
    sharingPolicy.changed(SHARING_POLICY.ID, false)
    sql.insertInto(SHARING_POLICY).set(sharingPolicy).execute()
    sharingPolicy
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
    val startTime = System.currentTimeMillis
    
    val total = sql.selectCount().from(SHARING_POLICY).where(SHARING_POLICY.SHARED_WITH.equal(sharedWith)).fetchOne(0, classOf[Int])
    
    val records = 
      sql.selectFrom(SHARING_POLICY
           .join(DOCUMENT)
           .on(SHARING_POLICY.DOCUMENT_ID.equal(DOCUMENT.ID)))
         .where(SHARING_POLICY.SHARED_WITH.equal(sharedWith))
         .limit(limit)
         .offset(offset)
         .fetchArray.toSeq
         .map(r => (r.into(classOf[DocumentRecord]), r.into(classOf[SharingPolicyRecord])))

    Page(System.currentTimeMillis - startTime, total, offset, limit, records)
  }
  
}
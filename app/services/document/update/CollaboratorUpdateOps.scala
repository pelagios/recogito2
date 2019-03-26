package services.document.update

import java.sql.Timestamp
import java.util.Date
import services.SharingLevel
import services.document.DocumentService
import services.generated.Tables.SHARING_POLICY
import services.generated.tables.records.SharingPolicyRecord

trait CollaboratorUpdateOps { self: DocumentService => 

  /** Upserts a document collaborator sharing policy (policies are unique by (document_id, shared_with) **/
  def addDocumentCollaborator(documentId: String, sharedBy: String, sharedWith: String, level: SharingLevel) = db.query { sql =>
    val (sharingPolicy, isNewCollaborator) = 
      Option(sql.selectFrom(SHARING_POLICY)
                .where(SHARING_POLICY.DOCUMENT_ID.equal(documentId)
                  .and(SHARING_POLICY.SHARED_WITH.equal(sharedWith)))
                .fetchOne()) match {
      
      case Some(policy) => {
        // There's a policy for this document/user pair already - update
        policy.setSharedBy(sharedBy)
        policy.setSharedAt(new Timestamp(new Date().getTime))
        policy.setAccessLevel(level.toString)
        (policy, false)
      }
        
      case None => {
        // Create new sharing policy
        val policy = new SharingPolicyRecord(null, null,
          documentId, 
          sharedBy,
          sharedWith,
          new Timestamp(new Date().getTime),
          level.toString)
   
        policy.changed(SHARING_POLICY.ID, false)     
        sql.attach(policy)
        (policy, true)
      }
      
    }
    
    sharingPolicy.store()
    (sharingPolicy, isNewCollaborator)
  } 

  /** Removes a document collaborator sharing policy **/
  def removeDocumentCollaborator(documentId: String, sharedWith: String) = db.query { sql =>
    sql.deleteFrom(SHARING_POLICY)
       .where(SHARING_POLICY.DOCUMENT_ID.equal(documentId)
         .and(SHARING_POLICY.SHARED_WITH.equal(sharedWith)))
       .execute() == 1
  } 

}
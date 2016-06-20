package models.sharing

import models.generated.Tables._
import storage.DB

object SharingPolicyService {
  
  def listDocumentCollaborators(documentId: String)(implicit db: DB) = db.query { sql =>
    sql.selectFrom(SHARING_POLICY).where(SHARING_POLICY.DOCUMENT_ID.equal(documentId)).fetchArray().toSeq
  }
  
}
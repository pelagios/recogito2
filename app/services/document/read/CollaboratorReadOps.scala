package services.document.read

import services.document.DocumentService
import services.generated.Tables.SHARING_POLICY

trait CollaboratorReadOps { self: DocumentService => 

  /** Lists all collaborators on a specific document **/
  def listDocumentCollaborators(documentId: String) = db.query { sql =>
    sql.selectFrom(SHARING_POLICY)
       .where(SHARING_POLICY.DOCUMENT_ID.equal(documentId))
       .fetchArray().toSeq
  }

}
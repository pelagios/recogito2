package services.document.read

import java.util.UUID
import services.document.DocumentService
import services.generated.Tables.DOCUMENT_FILEPART

trait FilepartReadOps { self: DocumentService =>

  /** Retrieves a part record by ID **/
  def findPartById(id: UUID) = db.query { sql => 
    Option(sql.selectFrom(DOCUMENT_FILEPART).where(DOCUMENT_FILEPART.ID.equal(id)).fetchOne())
  }

}
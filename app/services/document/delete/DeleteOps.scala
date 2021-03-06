package services.document.delete

import org.apache.commons.io.FileUtils
import scala.concurrent.{ExecutionContext, Future}
import services.document.DocumentService
import services.generated.Tables.{DOCUMENT, DOCUMENT_FILEPART, SHARING_POLICY}
import services.generated.tables.records.DocumentRecord
import storage.db.DB
import storage.uploads.Uploads

/** Document delete operations **/
trait DeleteOps { self: DocumentService =>

  /** Deletes a document by its ID, along with sharing policies and files **/
  def delete(
    document: DocumentRecord
  ): Future[Unit] = db.withTransaction { sql =>

    // Delete sharing policies
    sql.deleteFrom(SHARING_POLICY)
       .where(SHARING_POLICY.DOCUMENT_ID.equal(document.getId))
       .execute()

    // Delete filepart records
    sql.deleteFrom(DOCUMENT_FILEPART)
       .where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(document.getId))
       .execute()
    
    // Delete document records
    sql.deleteFrom(DOCUMENT)
       .where(DOCUMENT.ID.equal(document.getId))
       .execute()
       
    // Delete files - note: some documents may not have local files (e.g. IIIF)  
    val maybeDocumentDir = uploads.getDocumentDir(document.getOwner, document.getId)
    if (maybeDocumentDir.isDefined)
      FileUtils.deleteDirectory(maybeDocumentDir.get)
  }
  
  /** Bulk-deletes all documents owned by the given user **/
  def deleteByOwner(
    username: String
  )(implicit ctx: ExecutionContext) = db.withTransaction { sql =>

    // Delete sharing policies
    sql.deleteFrom(SHARING_POLICY)
       .where(SHARING_POLICY.DOCUMENT_ID.in(
         sql.select(DOCUMENT.ID).from(DOCUMENT).where(DOCUMENT.OWNER.equal(username))
       ))
       .execute()
    
    // Delete filepart records
    sql.deleteFrom(DOCUMENT_FILEPART)
       .where(DOCUMENT_FILEPART.DOCUMENT_ID.in(
         sql.select(DOCUMENT.ID).from(DOCUMENT).where(DOCUMENT.OWNER.equal(username))
       ))
       .execute()
       
    // Delete document records
    sql.deleteFrom(DOCUMENT)
       .where(DOCUMENT.OWNER.equal(username))
       .execute()
       
    // Delete files
    uploads.deleteUserDir(username)
  }
 
  /** Deletes all policies shared by and with the given user **/
  def deletePoliciesByUsername(username: String) = db.withTransaction { sql =>
    sql.deleteFrom(SHARING_POLICY)
      .where(SHARING_POLICY.SHARED_WITH.equal(username)
        .or(SHARING_POLICY.SHARED_BY.equal(username)))
      .execute()
  }

}
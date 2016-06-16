package models.document

import collection.JavaConversions._
import models.BaseService
import models.generated.Tables._
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord, UploadRecord }
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import play.api.Logger
import play.api.cache.CacheApi
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.language.postfixOps
import storage.{ DB, FileAccess }

case class PartOrdering(partId: Int, seqNo: Int)

object DocumentService extends BaseService with FileAccess {
  
  // We use random alphanumeric IDs with 14 chars length (because 62^14 should be enough for anyone (TM))  
  private val ID_LENGTH = 14
  
  private[document] def generateRandomID(retriesLeft: Int = 10)(implicit db: DB): String = {
    
    // Takes a set of strings and returns those that already exist in the DB as doc IDs
    def findIDs(ids: Set[String])(implicit db: DB) = db.query { sql =>
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
    val idsAlreadyInDB = Await.result(findIDs(randomIds), 10 seconds)    
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
  
  private def determineAccessLevel(document: DocumentRecord, forUser: Option[String]): DocumentAccessLevel = forUser match {      
    case Some(user) if (document.getOwner == user) => DocumentAccessLevel.OWNER

    // TODO check sharing policies

    case _ if (document.getIsPublic)               => DocumentAccessLevel.READ
    case _                                         => DocumentAccessLevel.FORBIDDEN
  }
  
  /** Creates a new DocumentRecord from an UploadRecord **/
  def createDocument(upload: UploadRecord)(implicit db: DB) =
    new DocumentRecord(
          generateRandomID(),
          upload.getOwner,
          upload.getCreatedAt,
          upload.getTitle,
          upload.getAuthor,
          null, // TODO timestamp_numeric
          upload.getDateFreeform,
          upload.getDescription,
          upload.getLanguage,
          upload.getSource,
          upload.getEdition,
          false)
  
  /** Changes the public visibility flag for the given document **/
  def setPublicVisibility(docId: String, enabled: Boolean)(implicit db: DB) = db.withTransaction { sql =>
    sql.update(DOCUMENT).set[java.lang.Boolean](DOCUMENT.IS_PUBLIC, enabled).where(DOCUMENT.ID.equal(docId)).execute()
  }
  
  /** Changes the sequence numbers of fileparts for a specific document **/
  def setFilepartSortOrder(docId: String, sortOrder: Seq[PartOrdering])(implicit db: DB) = db.withTransaction { sql =>
    // To verify validaty of the request, load the fileparts from the DB first...
    val fileparts = 
      sql.selectFrom(DOCUMENT_FILEPART).where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(docId)).fetchArray()
    
    // ...discard parts that are not associated with the document and log a warning
    val foundIds = fileparts.map(_.getId).toSet
    val requestedIds = sortOrder.map(_.partId).toSet
    if (requestedIds != foundIds)
      Logger.warn("Attempt to re-order fileparts that don't belong to the specified doc")
    val sanitizedOrder = sortOrder.filter(ordering => foundIds.contains(ordering.partId))
    
    // Should normally be empty
    val unchangedParts = fileparts.filter(part => !requestedIds.contains(part.getId))
    if (unchangedParts.size > 0)
      Logger.warn("Request for re-ordering fileparts is missing " + unchangedParts.size + " rows")
   
    // There is no uniquness constraint in the DB on (documentId, seqNo), since we wouldn't be able to
    // update sequence numbers without changing part IDs then. Therefore we enforce uniqueness here.
    val updatedSequenceNumbers = sanitizedOrder.map(_.seqNo) ++ unchangedParts.map(_.getSequenceNo)
    if (updatedSequenceNumbers.size != updatedSequenceNumbers.distinct.size)
      throw new Exception("Uniqueness constraint violated for Filepart (document_id, sequence_no)")
      
    // Update fileparts in DB
    val updates = sanitizedOrder.map(ordering =>
      sql.update(DOCUMENT_FILEPART)
         .set(DOCUMENT_FILEPART.SEQUENCE_NO, ordering.seqNo.asInstanceOf[java.lang.Integer])
         .where(DOCUMENT_FILEPART.ID.equal(ordering.partId)))

    sql.batch(updates:_*).execute()
  }
  
  /** Retrieves a document by its ID, along with access permissions for the given user **/
  def findById(id: String, loggedInUser: Option[String] = None)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(DOCUMENT).where(DOCUMENT.ID.equal(id)).fetchOne()).map(document =>
      (document, determineAccessLevel(document, loggedInUser)))
  }
  
  /** Retrieves a document by ID, along with fileparts **/
  def findByIdWithFileparts(id: String, loggedInUser: Option[String] = None)(implicit db: DB) = db.query { sql =>
    val records =
      sql.selectFrom(DOCUMENT
        .join(DOCUMENT_FILEPART)
        .on(DOCUMENT.ID.equal(DOCUMENT_FILEPART.DOCUMENT_ID)))
      .where(DOCUMENT.ID.equal(id))
      .fetchArray()

    // Convert to (DocumentRecord, Seq[DocumentFilepartRecord) tuple
    val grouped = groupLeftJoinResult(records, classOf[DocumentRecord], classOf[DocumentFilepartRecord])
    if (grouped.size > 1)
      throw new RuntimeException("Got " + grouped.size + " DocumentRecords with the same ID: " + grouped.keys.map(_.getId).mkString(", "))

    // Return with parts sorted by sequence number
    grouped
      .headOption
      .map { case (document, parts) =>
        (document, parts.sortBy(_.getSequenceNo), determineAccessLevel(document, loggedInUser)) }
  }

  /** Retrieves a filepart by document ID and sequence number **/
  def findPartByDocAndSeqNo(docId: String, seqNo: Int)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(DOCUMENT_FILEPART)
              .where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(docId))
              .and(DOCUMENT_FILEPART.SEQUENCE_NO.equal(seqNo))
              .fetchOne())
  }
  
  /** Retrieves documents by their owner **/
  def findByOwner(owner: String, offset: Int = 0, limit: Int = 20)(implicit db: DB) = db.query { sql =>
    sql.selectFrom(DOCUMENT)
       .where(DOCUMENT.OWNER.equal(owner))
       .limit(limit)
       .offset(offset)
       .fetchArray().toSeq
  }
  
  /** Deletes a document by its ID, along with filepart records and files **/
  def delete(document: DocumentRecord)(implicit db: DB): Future[Unit] = db.withTransaction { sql =>
    sql.deleteFrom(DOCUMENT_FILEPART)
       .where(DOCUMENT_FILEPART.DOCUMENT_ID.equal(document.getId))
       .execute()

    // Note: some documents may not have local files - e.g. IIIF  
    val maybeDocumentDir = getDocumentDir(document.getOwner, document.getId)
    if (maybeDocumentDir.isDefined)
      FileUtils.deleteDirectory(maybeDocumentDir.get)
    
    sql.deleteFrom(DOCUMENT)
       .where(DOCUMENT.ID.equal(document.getId))
       .execute()
  }

}

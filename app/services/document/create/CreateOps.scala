package services.document.create

import java.io.{File, InputStream}
import java.nio.file.Files
import java.sql.Timestamp
import java.util.{Date, UUID}
import org.jooq.DSLContext
import scala.concurrent.Future
import services.{ContentType, PublicAccess}
import services.document.{DocumentService, DocumentIdFactory}
import services.generated.Tables.{DOCUMENT, DOCUMENT_FILEPART}
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}

/** Helper to encapsulate doc and filepart Ids before and after cloning **/
case class CloneCorrespondence(
  docIdBefore: String,
  docIdAfter: String,
  filepartIds: Map[UUID, UUID])

trait CreateOps { self: DocumentService => 

  /** Imports document and filepart records to DB, and filepart content to user dir **/
  def importDocument(document: DocumentRecord, fileparts: Seq[(DocumentFilepartRecord, InputStream)]) = db.withTransaction { sql =>
    // Import DocumentRecord 
    sql.insertInto(DOCUMENT).set(document).execute()
    
    // Import DocumentFilepartRecords 
    val inserts = fileparts.map { case (part, _) => sql.insertInto(DOCUMENT_FILEPART).set(part) }    
    sql.batch(inserts:_*).execute()
    
    // Import files
    fileparts.foreach { case (part, stream) =>
      val destination = new File(uploads.getDocumentDir(document.getOwner, document.getId, true).get, part.getFile).toPath
      Files.copy(stream, destination)
    }
  }

  /** Creates a copy of the file associated with the given part 
    * 
    * Warning: this method strictly assumes that there IS a local
    * file for this filepart. Checking whether the 'file' field of 
    * the part points to a local file or a remote source has to 
    * happen outside of this method.
    */
  private def copyFile(
    origDoc: DocumentRecord, // The original document the source part is from
    origPart: DocumentFilepartRecord, // The original source part
    clonedDoc: DocumentRecord, // The cloned document record the new, cloned part should be part of
    clonedPartId: UUID // The ID the new, cloned part should have
  ): String = {
    val sourceFile = new File(
      uploads.getDocumentDir(origDoc.getOwner, origDoc.getId).get, 
      origPart.getFile
    ).toPath

    // Destination filename = new UUID + old file extension
    val sourceExtension = origPart.getFile.substring(origPart.getFile.lastIndexOf('.'))
    val destinationFilename = s"${clonedPartId}.${sourceExtension}"

    val destinationFile = new File(
      uploads.getDocumentDir(clonedDoc.getOwner, clonedDoc.getId, true).get,
      destinationFilename
    ).toPath

    Files.copy(sourceFile, destinationFile)

    destinationFile.getFileName.toString
  }

  /** Duplicates the given filepart record, copying the file as needed **/
  private def cloneFilepart(
    origDoc: DocumentRecord,
    clonedDoc: DocumentRecord,
    origPart: DocumentFilepartRecord
  )(implicit sql: DSLContext): DocumentFilepartRecord = {

    // Create a new UUID for the cloned filepart
    val clonedPartId = UUID.randomUUID

    // Depending on the content type, 'file' will either point to a copy of the local file, 
    // or the same remote source
    val origContentType = ContentType.withName(origPart.getContentType).get
    val clonedFile = 
      if (origContentType.isLocal) {
        copyFile(origDoc, origPart, clonedDoc, clonedPartId) 
      } else {
        origPart.getFile
      }

    val filepartRecord = new DocumentFilepartRecord(
      clonedPartId,
      clonedDoc.getId,
      origPart.getTitle,
      origPart.getContentType,
      clonedFile,
      origPart.getSequenceNo,
      origPart.getSource)

    sql.insertInto(DOCUMENT_FILEPART).set(filepartRecord).execute()
    filepartRecord
  }

  /** Clones the given document and fileparts
    *
    * Optionally, to a different user's workspace.
    */
  def cloneDocument(
    doc: DocumentRecord,
    fileparts: Seq[DocumentFilepartRecord],
    newOwner: Option[String] = None
  ): Future[CloneCorrespondence] = db.withTransaction { implicit sql =>
    val clonedDocId = DocumentIdFactory.generateRandomID()

    // Clone the document record
    val clonedDoc = newOwner match {
      case None => // Just clone inside users workspace
        new DocumentRecord(
          clonedDocId,
          doc.getOwner, // TODO make configurable later ("forking")
          new Timestamp(new Date().getTime),
          s"${doc.getTitle} (copy)",
          doc.getAuthor,
          doc.getDateNumeric,
          doc.getDateFreeform,
          doc.getDescription,
          doc.getLanguage,
          doc.getSource,
          doc.getEdition,
          doc.getLicense,
          doc.getAttribution,
          doc.getPublicVisibility,
          doc.getPublicAccessLevel,
          doc.getId)

      case Some(username) => // Clone to someone else's workspace
        new DocumentRecord(
          clonedDocId,
          username,
          new Timestamp(new Date().getTime),
          doc.getTitle,
          doc.getAuthor,
          doc.getDateNumeric,
          doc.getDateFreeform,
          doc.getDescription,
          doc.getLanguage,
          doc.getSource,
          doc.getEdition,
          doc.getLicense,
          doc.getAttribution,
          PublicAccess.PRIVATE.toString, // default visibility
          null, // default access level
          doc.getId)
    }

    sql.insertInto(DOCUMENT).set(clonedDoc).execute()

    // TODO folder association! (only for duplicates, not forks)

    // Clone fileparts (DB records + local files, if any)
    val clonedParts = fileparts.map { part => 
      cloneFilepart(doc, clonedDoc, part)
    }

    CloneCorrespondence(
      doc.getId,
      clonedDoc.getId,
      fileparts.zip(clonedParts).map { case (before, after) =>
        before.getId -> after.getId
      }.toMap)
  }

}
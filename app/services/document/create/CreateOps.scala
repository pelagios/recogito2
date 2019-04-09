package services.document.create

import java.io.{File, InputStream}
import java.nio.file.Files
import java.sql.Timestamp
import java.util.{Date, UUID}
import scala.concurrent.Future
import services.document.{DocumentService, DocumentIdFactory}
import services.generated.Tables.{DOCUMENT, DOCUMENT_FILEPART}
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord}

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

  /** TODO not all fileparts have files!! **/
  private def duplicateFilepart(origPart: DocumentFilepartRecord, origDoc: DocumentRecord, clonedDoc: DocumentRecord) = {
    val clonedPartId = UUID.randomUUID

    val sourceFile = new File(
      uploads.getDocumentDir(origDoc.getOwner, origDoc.getId).get, 
      origPart.getFile
    ).toPath

    val sourceExtension = origPart.getFile.substring(origPart.getFile.lastIndexOf('.'))
    val destinationFilename = s"${clonedPartId}.${sourceExtension}"

    val destinationFile = new File(
      uploads.getDocumentDir(clonedDoc.getOwner, clonedDoc.getId).get,
      destinationFilename
    ).toPath

    Files.copy(sourceFile, destinationFile)

    // TODO duplicate record
  }

  /** Duplicates a  */
  def duplicateDocument(
    doc: DocumentRecord,
    fileparts: Seq[DocumentFilepartRecord]
  ): Future[Boolean] /** TODO or document instead? **/ = {
    val clonedDocId = DocumentIdFactory.generateRandomID()

    // TODO place in same folder

    // Step 1 - clone the doc record
    val cloneDocument = new DocumentRecord(
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
      doc.getPublicAccessLevel)

    // TODO Step 2 - copy files


    // Step 3 - clone the filepart records
    val clonedParts = fileparts.map { part => 
      new DocumentFilepartRecord(
        UUID.randomUUID,
        clonedDocId,
        part.getTitle,
        part.getContentType,
        null, // TODO part.getFile -> use clone location
        part.getSequenceNo,
        part.getSource)
    }

    // Step 4 - annotations (note: history & contribution records will be ignored)

    ???

  }

}
package services.document.create

import java.io.{File, InputStream}
import java.nio.file.Files
import services.document.DocumentService
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

}
package models

import database.DB
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID
import models.generated.Tables._
import models.generated.tables.records.UploadRecord
import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.Files.TemporaryFile
import models.generated.tables.records.UploadFilepartRecord
import models.generated.tables.records.UploadFilepartRecord

object UploadService {
  
  private val UPLOAD_DIR = {
    val dir = Play.current.configuration.getString("recogito.upload.dir") match {
      case Some(filename) => new File(filename)   
      case None => new File("uploads") // Default
    }
    
    if (!dir.exists)
      dir.mkdir()
      
    dir
  }
  
  /** Inserts a new upload, or updates an existing one if it already exists **/
  def storeUpload(owner: String, title: String, author: String, dateFreeform: String, description: String, source: String, language: String)(implicit db: DB) =
    db.withTransaction { sql =>
      val upload = 
        Option(sql.selectFrom(UPLOAD).where(UPLOAD.OWNER.equal(owner)).fetchOne()) match {
          case Some(upload) => {
            // Pending upload - update
            upload.setCreatedAt(OffsetDateTime.now)
            upload.setTitle(title)
            upload.setAuthor(author)
            upload.setDateFreeform(dateFreeform)
            upload.setDescription(description)
            upload.setSource(source)
            upload.setLanguage(language)
            upload
          }
          
          case None => {
            // No pending upload - create new
            val upload = new UploadRecord(null, owner, OffsetDateTime.now, title, author, dateFreeform, description, source, language)
            sql.attach(upload)
            upload
          }
        }
      
      upload.store()
      upload
  }
  
  def findForUser(username: String)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(UPLOAD).where(UPLOAD.OWNER.equal(username)).fetchOne())
  }
  
  def findForUserWithFileparts(username: String)(implicit db: DB) = db.query { sql =>
    sql.selectFrom(UPLOAD
        .leftJoin(UPLOAD_FILEPART)
        .on(UPLOAD.ID.equal(UPLOAD_FILEPART.UPLOAD_ID)))
      .where(UPLOAD.OWNER.equal(username))
      .fetchArray().toSeq
  }
  
  def insertFilepart(uploadId: Int, filepart: FilePart[TemporaryFile])(implicit db: DB) = db.withTransaction { sql =>
    val title = filepart.filename
    val extension = title.substring(title.lastIndexOf('.'))
    val filepath = new File(UPLOAD_DIR, UUID.randomUUID.toString + extension)
    val filepartRecord = new UploadFilepartRecord(null, uploadId, title, filepath.getPath) 
    filepart.ref.moveTo(new File(s"$filepath"))
    sql.insertInto(UPLOAD_FILEPART).set(filepartRecord).execute
    filepartRecord
  }
    
}
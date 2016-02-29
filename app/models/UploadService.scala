package models

import collection.JavaConverters._
import java.io.File
import java.nio.file.{ Files, Paths, StandardCopyOption }
import java.time.OffsetDateTime
import java.util.UUID
import models.generated.Tables._
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord, UploadRecord, UploadFilepartRecord }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.Files.TemporaryFile
import play.api.Logger
import scala.collection.JavaConversions._
import storage.{ DB, FileAccess }

object UploadService extends FileAccess {

  /** Ugly helper that turns empty strings to null, so they are properly to the DB **/
  private def nullIfEmpty(s: String) = if (s.trim.isEmpty) null else s

  /** Inserts a new upload, or updates an existing one if it already exists **/
  def storePendingUpload(owner: String, title: String, author: String, dateFreeform: String, description: String, source: String, language: String)(implicit db: DB) =
    db.withTransaction { sql =>
      val upload =
        Option(sql.selectFrom(UPLOAD).where(UPLOAD.OWNER.equal(owner)).fetchOne()) match {
          case Some(upload) => {
            // Pending upload - update
            upload.setCreatedAt(OffsetDateTime.now)
            upload.setTitle(title)
            upload.setAuthor(nullIfEmpty(author))
            upload.setDateFreeform(nullIfEmpty(dateFreeform))
            upload.setDescription(nullIfEmpty(description))
            upload.setSource(nullIfEmpty(source))
            upload.setLanguage(nullIfEmpty(language))
            upload
          }

          case None => {
            // No pending upload - create new
            val upload = new UploadRecord(null,
                owner,
                OffsetDateTime.now,
                nullIfEmpty(title),
                nullIfEmpty(author),
                nullIfEmpty(dateFreeform),
                nullIfEmpty(description),
                nullIfEmpty(source),
                nullIfEmpty(language))

            sql.attach(upload)
            upload
          }
        }

      upload.store()
      upload
  }

  /** Inserts a new filepart - metadata goes to the DB, content to the pending-uploads dir **/
  def insertFilepart(uploadId: Int, owner: String, filepart: FilePart[TemporaryFile])(implicit db: DB) = db.withTransaction { sql =>
    val title = filepart.filename
    val extension = title.substring(title.lastIndexOf('.'))
    val filepath = new File(PENDING_UPLOADS_DIR, UUID.randomUUID.toString + extension)
    val filepartRecord = new UploadFilepartRecord(null, uploadId, owner, title, ContentTypes.TEXT_PLAIN.toString, filepath.getName)
    filepart.ref.moveTo(new File(s"$filepath"))
    sql.insertInto(UPLOAD_FILEPART).set(filepartRecord).execute()
    filepartRecord
  }
  
  /** Deletes a filepart - record is removed from the DB, file from the data directory **/
  def deleteFilepartByTitleAndOwner(title: String, owner: String)(implicit db: DB) = db.withTransaction { sql =>
    Option(sql.selectFrom(UPLOAD_FILEPART)
              .where(UPLOAD_FILEPART.TITLE.equal(title))
              .and(UPLOAD_FILEPART.OWNER.equal(owner))
              .fetchOne()) match {
      
      case Some(filepartRecord) => {
        val file = new File(PENDING_UPLOADS_DIR, filepartRecord.getFilename)
        file.delete()
        filepartRecord.delete() 
      }
      
      case None =>
        // Not possible from the UI!
        Logger.warn("Somebody trying to remove a non-existing upload filepart")
    }                 
  }

  /** Retrieves the pending upload for a user (if any) **/
  def findPendingUpload(username: String)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(UPLOAD).where(UPLOAD.OWNER.equal(username)).fetchOne())
  }
  
  /** Retrieves the pending upload for a user (if any) along with the filepart metadata records **/
  def findPendingUploadWithFileparts(username: String)(implicit db: DB) = db.query { sql =>
    val result =
      sql.selectFrom(UPLOAD
        .leftJoin(UPLOAD_FILEPART)
        .on(UPLOAD.ID.equal(UPLOAD_FILEPART.UPLOAD_ID)))
      .where(UPLOAD.OWNER.equal(username))
      .fetchArray()

      // Convert to map (Upload -> Seq[Filepart]), filtering out null records returned as result of the join
      .map(record =>
        (record.into(classOf[UploadRecord]), record.into(classOf[UploadFilepartRecord])))
      .groupBy(_._1)
      .mapValues(_.map(_._2).filter(_.getId != null).toSeq)

    // Result map can have 0 or 1 key - otherwise DB integrity is compromised
    if (result.size > 1)
      throw new RuntimeException("DB contains multiple pending uploads for user " + username)

    if (result.isEmpty)
      None
    else
      Some(result.head)
  }

  /** Promotes a pending upload in the staging area to actual document **/
  def importPendingUpload(upload: UploadRecord, fileparts: Seq[UploadFilepartRecord])(implicit db: DB) = db.withTransaction { sql =>
    val document = new DocumentRecord(null,
          upload.getOwner,
          upload.getCreatedAt,
          upload.getTitle,
          upload.getAuthor,
          null, // TODO timestamp_numeric
          upload.getDateFreeform,
          upload.getDescription,
          upload.getSource,
          upload.getLanguage)
    
    // Insert document, retrieving the auto-generated ID
    val docId = 
      sql.insertInto(DOCUMENT).set(document).returning(DOCUMENT.ID).fetchOne()
    document.setId(docId.getId)

    // Insert filepart records - I couldn't find a way to do a batch-insert that also returns
    // the auto-generated ID. Any hints on how this could be achieved appreciated! 
    val docFileparts = fileparts.map(part => {
      val docFilepart = new DocumentFilepartRecord(null,
            docId.getId,
            part.getTitle, 
            part.getContentType, 
            part.getFilename)

      val docFilepartId =
        sql.insertInto(DOCUMENT_FILEPART).set(docFilepart).returning(DOCUMENT_FILEPART.ID).fetchOne()
      docFilepart.setId(docFilepartId.getId)
      docFilepart
    })

    // Move files from 'pending' to 'user-data' folder
    val filePaths = fileparts.map(filepart => {
      val source = new File(PENDING_UPLOADS_DIR, filepart.getFilename).toPath
      val destination = new File(getUserDir(upload.getOwner, true).get, filepart.getFilename).toPath
      Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE)
    })
    
    // Delete Upload and Filepart records from the staging area tables
    sql.deleteFrom(UPLOAD_FILEPART).where(UPLOAD_FILEPART.UPLOAD_ID.equal(upload.getId)).execute()
    sql.deleteFrom(UPLOAD).where(UPLOAD.ID.equal(upload.getId)).execute()

    (document, docFileparts)
  }

}

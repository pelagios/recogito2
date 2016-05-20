package models.upload

import collection.JavaConverters._
import java.io.File
import java.nio.file.{ Files, Paths, StandardCopyOption }
import java.time.OffsetDateTime
import java.util.UUID
import models.ContentIdentificationFailures._
import models.generated.Tables._
import models.generated.tables.records.{ DocumentRecord, DocumentFilepartRecord, UploadRecord, UploadFilepartRecord }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.Files.TemporaryFile
import scala.collection.JavaConversions._
import scala.concurrent.Future
import storage.{ DB, FileAccess }
import models.document.DocumentService
import models.ContentType

object UploadService extends FileAccess {

  /** Java-interop helper that turns empty strings to null, so they are properly inserted by JOOQ **/
  private def nullIfEmpty(s: String) = if (s.trim.isEmpty) null else s

  /** Inserts a new upload, or updates an existing one if it already exists **/
  def storePendingUpload(owner: String, title: String, author: String, dateFreeform: String, description: String, source: String, language: String)(implicit db: DB) =
    db.withTransaction { sql =>
      val upload =
        Option(sql.selectFrom(UPLOAD).where(UPLOAD.OWNER.equal(owner)).fetchOne()) match {
          case Some(upload) => {
            // Pending upload exists - update
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
            upload.changed(UPLOAD.ID, false);
            upload
          }
        }

      upload.store()
      upload
  }

  /** Inserts a new filepart - metadata goes to the DB, content to the pending-uploads dir **/
  def insertFilepart(uploadId: Int, owner: String, filepart: FilePart[TemporaryFile])(implicit db: DB):
    Future[Either[ContentIdentificationFailure, UploadFilepartRecord]] = db.withTransaction { sql =>

    val title = filepart.filename
    val extension = title.substring(title.lastIndexOf('.'))
    val filesize = filepart.ref.file.length.toDouble / 1024
    val file = new File(PENDING_UPLOADS_DIR, UUID.randomUUID.toString + extension)

    ContentType.fromFile(file) match {
      case Right(contentType) => {
        filepart.ref.moveTo(file)
        val filepartRecord = new UploadFilepartRecord(null, uploadId, owner, title, contentType.toString, file.getName, filesize)
        filepartRecord.changed(UPLOAD_FILEPART.ID, false)
        
        sql.insertInto(UPLOAD_FILEPART).set(filepartRecord).execute()
        Right(filepartRecord)
      }

      case Left(identificationFailure) => Left(identificationFailure)
    }
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
        filepartRecord.delete() == 1
      }

      case None =>
        // Happens when someone clicks 'delete' on a failed upload - never mind
        false
    }
  }

  /** Retrieves the pending upload for a user (if any) **/
  def findPendingUpload(username: String)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(UPLOAD).where(UPLOAD.OWNER.equal(username)).fetchOne())
  }

  /** Deletes a user's pending upload **/
  def deletePendingUpload(username: String)(implicit db: DB) = db.query { sql =>
    val fileparts =
      sql.selectFrom(UPLOAD_FILEPART)
         .where(UPLOAD_FILEPART.OWNER.equal(username))
         .fetchArray

    fileparts.foreach(part => {
      val file = new File(PENDING_UPLOADS_DIR, part.getFilename)
      file.delete()
    })

    sql.deleteFrom(UPLOAD_FILEPART).where(UPLOAD_FILEPART.OWNER.equal(username)).execute
    sql.deleteFrom(UPLOAD).where(UPLOAD.OWNER.equal(username)).execute() == 1
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
    val document = DocumentService.createDocument(upload)

    sql.insertInto(DOCUMENT).set(document).execute()

    // Insert filepart records - I couldn't find a way to do a batch-insert that also returns
    // the auto-generated ID. Any hints on how this could be achieved appreciated!
    val docFileparts = fileparts.zipWithIndex.map { case (part, idx) => {
      val docFilepart = new DocumentFilepartRecord(null,
            document.getId,
            part.getTitle,
            part.getContentType,
            part.getFilename,
            idx + 1)

      docFilepart.changed(DOCUMENT_FILEPART.ID, false)
      val docFilepartId =
        sql.insertInto(DOCUMENT_FILEPART).set(docFilepart).returning(DOCUMENT_FILEPART.ID).fetchOne()
      docFilepart.setId(docFilepartId.getId)
      docFilepart
    }}

    // Move files from 'pending' to 'user-data' folder
    val filePaths = fileparts.map(filepart => {
      val source = new File(PENDING_UPLOADS_DIR, filepart.getFilename).toPath
      val destination = new File(getDocumentDir(upload.getOwner, document.getId, true).get, filepart.getFilename).toPath
      Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE)
    })

    // Delete Upload and Filepart records from the staging area tables
    sql.deleteFrom(UPLOAD_FILEPART).where(UPLOAD_FILEPART.UPLOAD_ID.equal(upload.getId)).execute()
    sql.deleteFrom(UPLOAD).where(UPLOAD.ID.equal(upload.getId)).execute()

    (document, docFileparts)
  }

}

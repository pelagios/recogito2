package models

import database.DB
import java.time.OffsetDateTime
import models.generated.Tables._
import models.generated.tables.records.UploadRecord
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object UploadService {
  
  def insertOrReplaceUpload(owner: String, title: String, author: String, dateFreeform: String, description: String, source: String, language: String)(implicit db: DB) =
    db.withTransaction { sql =>
      sql.delete(UPLOAD).where(UPLOAD.OWNER.equal(owner)).execute() // Make sure there's only one pending upload per user max
      val upload = new UploadRecord(null, owner, OffsetDateTime.now, title, author, dateFreeform, description, source, language)
      sql.insertInto(UPLOAD).set(upload).execute()
      upload
  }
  
  def findForUser(username: String)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(UPLOAD).where(UPLOAD.OWNER.equal(username)).fetchOne())
  }
  
}
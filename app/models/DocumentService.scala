package models

import database.DB
import java.time.OffsetDateTime
import models.generated.Tables._
import models.generated.tables.records.DocumentRecord
import models.generated.tables.records.UploadRecord
import models.generated.tables.records.UploadFilepartRecord

object DocumentService {

  def insertDocument(owner: String, author: String, title: String, description: String, language: String)(implicit db: DB) = db.query { sql =>
    val document = new DocumentRecord(null, owner, OffsetDateTime.now, author, title, null, null, description, null, language)
    sql.insertInto(DOCUMENT).set(document).execute()
    document
  }

}

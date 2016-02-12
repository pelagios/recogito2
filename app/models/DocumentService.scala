package models

import database.DB
import java.time.OffsetDateTime
import models.generated.Tables._
import models.generated.tables.records.DocumentRecord

object DocumentService {

  def insertMetadata(owner: String, author: String, title: String, description: String, language: String)(implicit db: DB) = db.withTransaction { sql =>
    val docMetadata = new DocumentRecord(null, owner, OffsetDateTime.now, author, title, null, null, description, null, language)
    sql.insertInto(DOCUMENT).set(docMetadata).execute()
  }

}

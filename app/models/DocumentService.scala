package models

import database.DB
import models.generated.Tables._
import models.generated.tables.records.DocumentsRecord

object Documents {
  
  def insertMetadata(owner: String, title: String, author: String, description: String, language: String)(implicit db: DB) = db.withTransaction { sql =>
    val docMetadata = new DocumentsRecord(null, owner, title, author, description, language)
    sql.insertInto(DOCUMENTS).set(docMetadata).execute()
  }
  
}
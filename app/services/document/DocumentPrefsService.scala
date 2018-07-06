package services.document

import scala.concurrent.Future
import services.generated.Tables.DOCUMENT_PREFERENCES

trait DocumentPrefsService { self: DocumentService =>
  
  def getPreferences(docId: String) = db.query { sql =>
    sql.selectFrom(DOCUMENT_PREFERENCES).where(DOCUMENT_PREFERENCES.DOCUMENT_ID.equal(docId)).fetchArray.toSeq
  }
  
  def upsertPreferences(docId: String, name: String, prefs: String): Future[Boolean] = db.query { sql =>
    /*
    sql
      .insertInto(DOCUMENT_PREFERENCES)
      .set(authorityFile)
      .onDuplicateKeyUpdate()
      .set(authorityFile)
      .execute()
     */
    ???
  }
  
}
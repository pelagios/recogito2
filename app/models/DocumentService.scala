package models

import database.DB
import models.generated.Tables._

object DocumentService {

  def findByUser(username: String, offset: Int = 0, limit: Int = 20)(implicit db: DB) = db.query { sql =>
    sql.selectFrom(DOCUMENT)
       .where(DOCUMENT.OWNER.equal(username))
       .limit(limit)
       .offset(offset)
       .fetchArray().toSeq
  }
  
}

package models

import models.generated.Tables._
import storage.DB

object DocumentService {

  def findById(id: Int)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(DOCUMENT).where(DOCUMENT.ID.equal(id)).fetchOne())
  }
  
  def findByUser(username: String, offset: Int = 0, limit: Int = 20)(implicit db: DB) = db.query { sql =>
    sql.selectFrom(DOCUMENT)
       .where(DOCUMENT.OWNER.equal(username))
       .limit(limit)
       .offset(offset)
       .fetchArray().toSeq
  }

}

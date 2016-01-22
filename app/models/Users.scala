package models

import database.DB
import models.generated.Tables._
import models.generated.tables.records.UsersRecord
import scala.collection.JavaConversions._

object Users {

  def listAll()(implicit db: DB) = db.query { sql =>
    sql.select().from(USERS).fetch().into(classOf[UsersRecord]).toSeq
  }

}

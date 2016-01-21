package models

import db.DB
import generated.Tables._
import generated.tables.records.UsersRecord
import scala.collection.JavaConversions._

object Users {

  def listAll()(implicit db: DB) = db.query { sql => 
    sql.select().from(USERS).fetch().into(classOf[UsersRecord]).toSeq
  }

}

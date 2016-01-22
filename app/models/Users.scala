package models

import database.DB
import models.generated.Tables._
import models.generated.tables.records.UsersRecord
import scala.collection.JavaConversions._
import java.time.OffsetDateTime

object Users {

  def listAll()(implicit db: DB) = db.query { sql =>
    sql.select().from(USERS).fetch().into(classOf[UsersRecord]).toSeq
  }
  
  def createUser(username:String, password:String)(implicit db: DB) = db.withTransaction {
    sql => sql.insertInto(USERS, USERS.USERNAME,USERS.EMAIL,USERS.MEMBER_SINCE)
      .values(username,password,OffsetDateTime.now())
      .execute()
  }

}

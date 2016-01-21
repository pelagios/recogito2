package models

import db.DB
import javax.inject.Inject    
import generated.tables.records.UsersRecord
 
class User @Inject() (db: DB) {
  
  import generated.Tables._

  def listAll() = db.query { sql => 
    sql.select().from(USERS).fetch().into(classOf[UsersRecord])
  }

}

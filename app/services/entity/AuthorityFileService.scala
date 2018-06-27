package services.entity

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import services.generated.Tables.AUTHORITY_FILE
import storage.db.DB
import services.generated.tables.records.AuthorityFileRecord

@Singleton
class AuthorityFileService @Inject() (val db: DB, implicit val ctx: ExecutionContext) {
  
  def listAll() = db.query { sql =>
    sql.selectFrom(AUTHORITY_FILE).fetchArray().toSeq
  }
  
  def findById(id: String) = db.query { sql =>
    Option(sql.selectFrom(AUTHORITY_FILE).where(AUTHORITY_FILE.ID.equal(id)).fetchOne())
  }
  
  def insert(
    id: String,
    screenName: String,
    shortcode: String,
    color: String,
    urlPatterns: Seq[String]
  ) = db.query { sql =>
    val authorityFile = 
      new AuthorityFileRecord(id, screenName, shortcode, color, urlPatterns.mkString(","))
    
    sql.insertInto(AUTHORITY_FILE).set(authorityFile).execute()
  }
  
}
package services.entity

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import services.generated.Tables.AUTHORITY_FILE
import storage.db.DB
import services.BaseService
import services.generated.tables.records.AuthorityFileRecord

@Singleton
class AuthorityFileService @Inject() (val db: DB, implicit val ctx: ExecutionContext) extends BaseService {
  
  def listAll() = db.query { sql =>
    sql.selectFrom(AUTHORITY_FILE).fetchArray().toSeq
  }
  
  def findById(id: String) = db.query { sql =>
    Option(sql.selectFrom(AUTHORITY_FILE).where(AUTHORITY_FILE.ID.equal(id)).fetchOne())
  }
  
  def upsert(
    identifier: String,
    shortname: String,
    fullname: Option[String],
    shortcode: Option[String],
    color: Option[String],
    urlPatterns: Seq[String]
  ) = db.query { sql =>
    val authorityFile = new AuthorityFileRecord(
      identifier, 
      shortname,
      optString(fullname),
      optString(shortcode),
      optString(color),
      { if (urlPatterns.size > 0) urlPatterns.mkString(",")
         else null })      
    
    sql
      .insertInto(AUTHORITY_FILE)
      .set(authorityFile)
      .onDuplicateKeyUpdate()
      .set(authorityFile)
      .execute()
  }
  
}
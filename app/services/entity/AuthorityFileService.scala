package services.entity

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext
import services.BaseService
import services.generated.Tables.AUTHORITY_FILE
import services.generated.tables.records.AuthorityFileRecord
import storage.db.DB

@Singleton
class AuthorityFileService @Inject() (val db: DB, implicit val ctx: ExecutionContext) extends BaseService {
  
  def listAll(eType: Option[EntityType] = None) = db.query { sql =>
    sql.selectFrom(AUTHORITY_FILE).fetchArray().toSeq
  }
  
  def findById(id: String) = db.query { sql =>
    Option(sql.selectFrom(AUTHORITY_FILE).where(AUTHORITY_FILE.ID.equal(id)).fetchOne())
  }
  
  def upsert(
    identifier: String,
    entityType: EntityType,
    shortname: String,
    fullname: Option[String],
    homepage: Option[String],
    shortcode: Option[String],
    color: Option[String],
    urlPatterns: Seq[String]
  ) = db.query { sql =>
    val authorityFile = new AuthorityFileRecord(
      identifier, 
      entityType.toString,
      shortname,
      optString(fullname),
      optString(homepage),
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
  
  def delete(identifier: String) = db.query { sql =>
    sql
      .deleteFrom(AUTHORITY_FILE)
      .where(AUTHORITY_FILE.ID.equal(identifier))
      .execute() == 1
  }
  
}

object AuthorityFileService {
  
  implicit val authorityFileRecordWrites: Writes[AuthorityFileRecord] = (
    (JsPath \ "identifier").write[String] and
    (JsPath \ "authority_type").write[String] and
    (JsPath \ "shortname").write[String] and
    (JsPath \ "fullname").writeNullable[String] and
    (JsPath \ "homepage").writeNullable[String] and
    (JsPath \ "shortcode").writeNullable[String] and
    (JsPath \ "color").writeNullable[String] and
    (JsPath \ "url_patterns").writeNullable[Seq[String]]
  )(a => (
      a.getId,
      a.getAuthorityType,
      a.getShortname,
      Option(a.getFullname),
      Option(a.getHomepage),
      Option(a.getShortcode),
      Option(a.getColor),
      Option(a.getUrlPatterns).map(_.split(","))
    ))
  
}
package models.user

import controllers.HasConfig
import java.math.BigInteger
import java.security.MessageDigest
import java.sql.Timestamp
import java.util.Date
import javax.inject.{ Inject, Singleton }
import models.{ BaseService, Page, SortOrder }
import models.user.Roles.Role
import models.generated.Tables._
import models.generated.tables.records.{ UserRecord, UserRoleRecord }
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import play.api.Configuration
import play.api.cache.CacheApi
import scala.collection.JavaConversions._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Either, Left, Right }
import storage.{ DB, Uploads }
import sun.security.provider.SecureRandom

@Singleton
class UserService @Inject() (
    val config: Configuration,
    val uploads: Uploads,
    implicit val cache: CacheApi,
    implicit val ctx: ExecutionContext,
    implicit val db: DB
  ) extends BaseService with HasConfig with HasEncryption {

  private val DEFAULT_QUOTA = config.getInt("recogito.upload.quota").getOrElse(200) // The default default

  def countUsers() = db.query { sql =>
    sql.selectCount().from(USER).fetchOne(0, classOf[Int])
  }

  def listUsers(offset: Int = 0, limit: Int = 20, sortBy: Option[String], sortOrder: Option[SortOrder]) = db.query { sql =>
    val startTime = System.currentTimeMillis

    val sortField = sortBy.flatMap(fieldname => getSortField(Seq(USER), fieldname, sortOrder))

    val total = sql.selectCount().from(USER).fetchOne(0, classOf[Int])
    
    val query = sortField match {
      case Some(field) => sql.selectFrom(USER).orderBy(field)
      case None => sql.selectFrom(USER)
    }
    
    val users = query.limit(limit).offset(offset).fetch().into(classOf[UserRecord])
    Page(System.currentTimeMillis - startTime, total, offset, limit, users.toSeq)
  }

  def insertUser(username: String, email: String, password: String) = db.withTransaction { sql =>
    val salt = randomSalt()
    val user = new UserRecord(username, encrypt(email), computeHash(salt + password), salt,
      new Timestamp(new Date().getTime), null, null, null, DEFAULT_QUOTA, true)
    sql.insertInto(USER).set(user).execute()
    user
  }
  
  def insertUserRole(username: String, role: Role) = db.withTransaction { sql =>
    sql.insertInto(USER_ROLE)
      .set(USER_ROLE.USERNAME, username)
      .set(USER_ROLE.HAS_ROLE, role.toString)
      .execute()
  }

  def resetPassword(username: String): Future[String] = db.withTransaction { sql =>
    val randomPassword = RandomStringUtils.randomAlphanumeric(18)
    val salt = randomSalt()
    sql.update(USER)
      .set(USER.PASSWORD_HASH, computeHash(salt + randomPassword))
      .set(USER.SALT, salt)
      .where(USER.USERNAME.equal(username))
      .execute()

    removeFromCache("user", username)
    
    randomPassword
  }

  def updatePassword(username: String, currentPassword: String, newPassword: String): Future[Either[String, Unit]] = db.withTransaction { sql =>
    Option(sql.selectFrom(USER).where(USER.USERNAME.equal(username)).fetchOne()) match {
      case Some(user) => {
        val isValid = computeHash(user.getSalt + currentPassword) == user.getPasswordHash
        if (isValid) {
          // User credentials OK - update password
          val salt = randomSalt()
          val rows =
            sql.update(USER)
              .set(USER.PASSWORD_HASH, computeHash(salt + newPassword))
              .set(USER.SALT, salt)
              .where(USER.USERNAME.equal(username))
              .execute()
              
          removeFromCache("user", username)
          
          Right(Unit)
        } else {
          // User failed password validation
          Left("Invalid Password")
        }
      }

      case None =>
        throw new Exception("Attempt to update password on unknown username")
    }
  }

  def updateUserSettings(username: String, email: String, realname: Option[String], bio: Option[String], website: Option[String]) = db.withTransaction { sql =>
    val rows =
      sql.update(USER)
        .set(USER.EMAIL, encrypt(email))
        .set(USER.REAL_NAME, realname.getOrElse(null))
        .set(USER.BIO, bio.getOrElse(null))
        .set(USER.WEBSITE, website.getOrElse(null))
        .where(USER.USERNAME.equal(username))
        .execute()

    removeFromCache("user", username)

    rows == 1
  }

  /** This method is cached, since it's basically called on every request **/
  def findByUsername(username: String) =
    cachedLookup("user", username, findByUsernameNoCache(_ , false))

  /** We're not caching at the moment, since it's not called often & would complicate matters **/
  def findByUsernameIgnoreCase(username: String) =
    findByUsernameNoCache(username, true)

  def findByUsernameNoCache(username: String, ignoreCase: Boolean) = db.query { sql =>
    val base = sql.selectFrom(USER.naturalLeftOuterJoin(USER_ROLE))
    val records =
      if (ignoreCase)
        base.where(USER.USERNAME.equalIgnoreCase(username)).fetchArray()
      else
        base.where(USER.USERNAME.equal(username)).fetchArray()

    groupLeftJoinResult(records, classOf[UserRecord], classOf[UserRoleRecord]).headOption
      .map { case (user, roles) => UserWithRoles(user, roles) }
  }
  
  def deleteByUsername(username: String) = db.withTransaction { sql =>
    sql.deleteFrom(USER_ROLE).where(USER_ROLE.USERNAME.equal(username)).execute()
    sql.deleteFrom(USER).where(USER.USERNAME.equal(username)).execute()
    removeFromCache("user", username)
  }
  
  def findByEmail(email: String) = db.query { sql =>
    Option(sql.selectFrom(USER).where(USER.EMAIL.equalIgnoreCase(encrypt(email))).fetchOne())
  }

  def validateUser(username: String, password: String): Future[Option[UserRecord]] = {
    val f =
      if (username.contains("@"))
        findByEmail(username)
      else
        findByUsername(username).map(_.map(_.user))
      
    f.map {
      case Some(user) =>
        val isValid = computeHash(user.getSalt + password) == user.getPasswordHash
        if (isValid) Some(user) else None
      
      case None => None
    }
  }

  /** Runs a prefix search on usernames.
    *
    * To keep result size low (and add some extra 'privacy') the method only matches on
    * usernames that are at most 2 characters longer than the query.
    */
  def searchUsers(query: String): Future[Seq[String]] = db.query { sql =>
    if (query.size > 2)
      sql.selectFrom(USER)
         .where(USER.USERNAME.like(query + "%")
           .and(USER.USERNAME.length().lt(query.size + 8)))
         .fetch()
         .getValues(USER.USERNAME, classOf[String]).toSeq
    else
      Seq.empty[String]
  }

  def decryptEmail(email: String) = decrypt(email)

  /** Utility function to create new random salt for password hashing **/
  private def randomSalt() = {
    val r = new SecureRandom()
    val salt = new Array[Byte](32)
    r.engineNextBytes(salt)
    Base64.encodeBase64String(salt)
  }

  /** Utility function to compute an MD5 password hash **/
  private def computeHash(str: String) = {
    val md = MessageDigest.getInstance("SHA-256").digest(str.getBytes)
    new BigInteger(1, md).toString(16)
  }

}

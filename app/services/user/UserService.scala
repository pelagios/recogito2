package services.user

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import controllers.HasConfig
import java.math.BigInteger
import java.security.MessageDigest
import java.sql.Timestamp
import java.util.Date
import javax.inject.{Inject, Singleton}
import services.{BaseService, Page, SortOrder}
import services.user.Roles.Role
import services.generated.Tables._
import services.generated.tables.records.{UserRecord, UserRoleRecord, FeatureToggleRecord}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.{Record, SelectWhereStep, DatePart}
import play.api.Configuration
import play.api.cache.SyncCacheApi
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Either, Left, Right}
import storage.db.DB
import storage.uploads.Uploads
import sun.security.provider.SecureRandom

@Singleton
class UserService @Inject() (
    val config: Configuration,
    val uploads: Uploads,
    implicit val cache: SyncCacheApi,
    implicit val ctx: ExecutionContext,
    implicit val db: DB
  ) extends BaseService with HasConfig with HasEncryption with IdentityService[User] {

  private val DEFAULT_QUOTA = config.getOptional[Int]("recogito.upload.quota").getOrElse(200) // The default default

  // Required by Silhouette auth framework
  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = findByUsername(loginInfo.providerKey)
  
  def countUsers() = db.query { sql =>
    sql.selectCount().from(USER).fetchOne(0, classOf[Int])
  }

  def listUsers(offset: Int = 0, limit: Int = 20, sortBy: Option[String], sortOrder: Option[SortOrder]) = db.query { sql =>
    val startTime = System.currentTimeMillis

    val sortField = sortBy.flatMap(fieldname => getSortField(Seq(USER), fieldname, sortOrder))
    val total = sql.selectCount().from(USER).fetchOne(0, classOf[Int])

    val query = sortField match {
      case Some(field) => 
        sql.select()
           .from(USER)
           .fullOuterJoin(USER_ROLE)
           .on(USER.USERNAME.equal(USER_ROLE.USERNAME))  
           .orderBy(field)

      case None => 
        sql.select()
           .from(USER)
           .fullOuterJoin(USER_ROLE)
           .on(USER.USERNAME.equal(USER_ROLE.USERNAME))  
    }

    val users = query.limit(limit).offset(offset).fetchArray().map { record => 
      val user = record.into(classOf[UserRecord])
      val isAdmin = record.getValue(USER_ROLE.HAS_ROLE) == "ADMIN"
      (user, isAdmin)
    }
    Page(System.currentTimeMillis - startTime, total, offset, limit, users.toSeq)
  }
  
  def listIdleUsers(since: Timestamp, offset: Int = 0, limit: Int = 20) = db.query {  sql =>
    val startTime = System.currentTimeMillis
    
    val total = sql.selectCount()
      .from(USER)
      .where(USER.LAST_LOGIN.lessThan(since))
      .fetchOne(0, classOf[Int])
      
    // We'll often use this just to count, so make to skip to 2nd query unless needed
    val users = 
      if (limit == 0)
        Seq.empty[UserRecord]
      else
        sql.selectFrom(USER)
          .where(USER.LAST_LOGIN.lessThan(since))
          .limit(limit)
          .offset(offset)
          .fetch().into(classOf[UserRecord]).toSeq
    
    Page(System.currentTimeMillis - startTime, total, offset, limit, users)
  } 

  def insertUser(username: String, email: String, password: String, optIn: Boolean) = db.withTransaction { sql =>
    val now = new Timestamp(new Date().getTime)
    val salt = randomSalt()
    val user = new UserRecord(username, encrypt(email), computeHash(salt + password), salt,
      now, null, null, null, DEFAULT_QUOTA, now, optIn, null)
    sql.insertInto(USER).set(user).execute()
    user
  }

  def insertUserRole(username: String, role: Role) = db.withTransaction { sql =>
    sql.insertInto(USER_ROLE)
      .set(USER_ROLE.USERNAME, username)
      .set(USER_ROLE.HAS_ROLE, role.toString)
      .execute()
  }

  /** For convenience **/
  def makeAdmin(username: String,  makeAdmin: Boolean) = db.withTransaction { sql => 
    val isAlreadyAdmin =
      sql
        .select(USER_ROLE.HAS_ROLE)
        .from(USER_ROLE)
        .where(USER_ROLE.USERNAME.equal(username))
        .fetchInto(classOf[String])
        .contains("ADMIN")

     if (!isAlreadyAdmin && makeAdmin)
       sql.insertInto(USER_ROLE)
         .set(USER_ROLE.USERNAME, username)
         .set(USER_ROLE.HAS_ROLE, "ADMIN")
         .execute()
     else if (isAlreadyAdmin && !makeAdmin)
       sql.delete(USER_ROLE)
         .where(USER_ROLE.USERNAME.equal(username)
           .and(USER_ROLE.HAS_ROLE.equal("ADMIN")))
         .execute()
     else
       0 // Rows affected
  }
  
  def updateLastLogin(username: String) = db.withTransaction { sql =>
    sql.update(USER)
      .set(USER.LAST_LOGIN, new Timestamp(new Date().getTime))
      .where(USER.USERNAME.equal(username))
      .execute()
  }

  def updateQuota(username: String, quota: Integer) = db.withTransaction { sql => 
    sql.update(USER)
      .set(USER.QUOTA_MB, quota)
      .where(USER.USERNAME.equal(username))
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

  def setReadme(username: String, readme: String): Future[Boolean] =
    db.withTransaction { sql =>
      val rows = 
        sql.update(USER)
          .set(USER.README, readme)
          .where(USER.USERNAME.equal(username))
          .execute

      removeFromCache("user", username)

      rows == 1
    }

  def deleteReadme(username: String): Future[Boolean] = setReadme(username, null);

  /** This method is cached, since it's basically called on every request **/
  def findByUsername(username: String) =
    cachedLookup("user", username, findByUsernameNoCache(_ , false))

  /** We're not caching at the moment, since it's not called often & would complicate matters **/
  def findByUsernameIgnoreCase(username: String) =
    findByUsernameNoCache(username, true)

  def findByUsernameNoCache(username: String, ignoreCase: Boolean) = db.query { sql =>

    def build(s : SelectWhereStep[Record]) =
      if (ignoreCase)
        s.where(USER.USERNAME.equalIgnoreCase(username))
      else
        s.where(USER.USERNAME.equal(username))

    val queryA = build(sql.selectFrom(USER.naturalLeftOuterJoin(USER_ROLE)))
    val queryB = build(sql.selectFrom(USER.naturalJoin(FEATURE_TOGGLE)))

    val recordsA = queryA.fetchArray
    val recordsB = queryB.fetchArray

    val userWithRoles = groupLeftJoinResult(recordsA, classOf[UserRecord], classOf[UserRoleRecord]).headOption
    val featureToggles = recordsB.map(_.into(classOf[FeatureToggleRecord])).toSeq
    userWithRoles.map { case (user, roles) =>
      User(user, roles, featureToggles)
    }
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
        findByUsername(username).map(_.map(_.record))

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
  def searchUsers(query: String, limit: Int = 20): Future[Seq[String]] = db.query { sql =>
    if (query.size > 2)
      sql.selectFrom(USER)
         .where(USER.USERNAME.lower.like(query.toLowerCase + "%")
           .and(USER.USERNAME.length().lt(query.size + 8)))
         .limit(limit)
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

  def getSignupsOverTime() = db.query { sql =>

    import org.jooq.impl.DSL.{count, trunc}
    
    val perDay = trunc(USER.MEMBER_SINCE, DatePart.DAY).as("per_day")
    sql.select(perDay, count(USER.USERNAME))
        .from(USER)
        .groupBy(perDay)
        .orderBy(perDay)
        .fetchArray.toSeq
        .foldLeft(Seq.empty[(Timestamp, Int)]) { (series, record) =>
          val timestamp = record.getValue(0, classOf[Timestamp])
          val count = record.getValue(1, classOf[Integer]).toInt

          series match {
            case Seq() => Seq((timestamp, count))
            case _ => series :+ (timestamp, series.last._2 + count)
          }
        }
  }

}

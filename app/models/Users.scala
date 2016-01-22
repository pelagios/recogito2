package models

import database.DB
import java.math.BigInteger
import java.security.MessageDigest
import java.time.OffsetDateTime
import models.generated.Tables._
import models.generated.tables.records.UsersRecord
import org.apache.commons.codec.binary.Base64
import scala.collection.JavaConversions._
import sun.security.provider.SecureRandom

object Users {
  
  private val MD5 = "MD5"

  def insertUser(username: String, email: String, password: String)(implicit db: DB) = db.withTransaction { sql =>
    val salt = randomSalt
    sql.insertInto(USERS, USERS.USERNAME, USERS.EMAIL, USERS.PASSWORD_HASH, USERS.SALT, USERS.MEMBER_SINCE)
      .values(username, email, computeHash(salt + password), salt, OffsetDateTime.now)
      .execute()
  }

  def listAll()(implicit db: DB) = db.query { sql =>
    sql.select().from(USERS).fetch().into(classOf[UsersRecord]).toSeq
  }

  /** Utility function to create new random salt for password hashing **/
  private def randomSalt = {
    val r = new SecureRandom()
    val salt = new Array[Byte](32)
    r.engineNextBytes(salt)
    Base64.encodeBase64String(salt)
  }

  /** Utility function to compute an MD5 password hash **/
  private def computeHash(str: String) = {
    val md = MessageDigest.getInstance(MD5).digest(str.getBytes)
    new BigInteger(1, md).toString(16)
  }

}

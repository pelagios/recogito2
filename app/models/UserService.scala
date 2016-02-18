package models

import database.DB
import java.math.BigInteger
import java.security.MessageDigest
import java.time.OffsetDateTime
import models.generated.Tables._
import models.generated.tables.records.UserRecord
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import sun.security.provider.SecureRandom

object UserService extends FileUsingService {

  private val SHA_256 = "SHA-256"

  def insertUser(username: String, email: String, password: String)(implicit db: DB) = db.withTransaction { sql =>
    val salt = randomSalt
    val user = new UserRecord(username, email, computeHash(salt + password), salt, OffsetDateTime.now, true)
    sql.insertInto(USER).set(user).execute()
    user
  }

  /** TODO this is accessed for every request - should cache this at some point **/
  def findByUsername(username: String)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(USER).where(USER.USERNAME.equal(username)).fetchOne())
  }
  
  def findByUsernameIgnoreCase(username: String)(implicit db: DB) = db.query { sql =>
    Option(sql.selectFrom(USER).where(USER.USERNAME.equalIgnoreCase(username)).fetchOne())
  }

  def validateUser(username: String, password: String)(implicit db: DB) =
    findByUsername(username).map(_ match {
      case Some(user) => computeHash(user.getSalt+password) == user.getPasswordHash
      case None => false
    })
    
  def getUsedDiskspaceKB(username: String) =
    getUserDir(username).map(dataDir => FileUtils.sizeOfDirectory(dataDir)).getOrElse(0l)

  /** Utility function to create new random salt for password hashing **/
  private def randomSalt = {
    val r = new SecureRandom()
    val salt = new Array[Byte](32)
    r.engineNextBytes(salt)
    Base64.encodeBase64String(salt)
  }

  /** Utility function to compute an MD5 password hash **/
  private def computeHash(str: String) = {
    val md = MessageDigest.getInstance(SHA_256).digest(str.getBytes)
    new BigInteger(1, md).toString(16)
  }

}

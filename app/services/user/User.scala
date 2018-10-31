package services.user

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import controllers.Security
import java.sql.Timestamp
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.generated.tables.records.{FeatureToggleRecord, UserRecord, UserRoleRecord}

case class User(
    
  val record: UserRecord, private val roleRecords: Seq[UserRoleRecord], private val featureToggleRecords: Seq[FeatureToggleRecord]
  
) extends Identity {
  
  val username = record.getUsername
  
  val email = record.getEmail
  
  val passwordHash = record.getPasswordHash
  
  val salt = record.getSalt
  
  val memberSince = record.getMemberSince
  
  val realName = Option(record.getRealName)
  
  val bio = Option(record.getBio)
  
  val website = Option(record.getWebsite)
  
  val quotaMb = record.getQuotaMb
    
  val featureToggles = featureToggleRecords.map(_.getHasToggle)
  
  val loginInfo = LoginInfo(Security.PROVIDER_ID, username) // Required by Silhouette auth framework
  
  def hasRole(role: Roles.Role): Boolean = roleRecords.exists(_.getHasRole == role.toString)

}

object Roles {

  sealed trait Role

  case object Admin extends Role { override lazy val toString = "ADMIN" }

  case object Normal extends Role { override lazy val toString = "NORMAL" }

}

object User {
  
  implicit val userRecordWrites: Writes[UserRecord] = (
    (JsPath \ "username").write[String] and
    (JsPath \ "realname").write[String] and
    (JsPath \ "member_since").write[Timestamp] and
    (JsPath \ "quota_mb").write[Int]
  )(u => (
    u.getUsername,
    u.getRealName,
    u.getMemberSince,
    u.getQuotaMb
  ))
  
}

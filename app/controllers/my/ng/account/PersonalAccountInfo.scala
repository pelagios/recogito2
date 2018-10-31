package controllers.my.ng.account

import java.sql.Timestamp
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.user.User

/** Personal account info.
  * 
  * Contains information that should not be visible
  * visiting users.
  */
case class PersonalAccountInfo(
  user: User, 
  myDocumentsCount: Long, 
  sharedWithMeCount: Long,
  usedMb: Double)

object PersonalAccountInfo {

  implicit val personalAccountInfoWrites: Writes[PersonalAccountInfo] = (
    (JsPath \ "username").write[String] and
    (JsPath \ "real_name").writeNullable[String] and
    (JsPath \ "member_since").write[Timestamp] and
    (JsPath \ "bio").writeNullable[String] and
    (JsPath \ "website").writeNullable[String] and
    (JsPath \ "documents").write[JsObject] and
    (JsPath \ "storage").write[JsObject]
  )(p => (
      p.user.username,
      p.user.realName,
      p.user.memberSince,
      p.user.bio,
      p.user.website,
      Json.obj(
        "my_documents" -> p.myDocumentsCount,
        "shared_with_me" -> p.sharedWithMeCount
      ),
      Json.obj(
        "quota_mb" -> p.user.quotaMb.toInt,
        "used_mb" -> p.usedMb
      )
  ))

}
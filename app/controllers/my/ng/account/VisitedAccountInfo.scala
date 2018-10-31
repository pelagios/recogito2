package controllers.my.ng.account

import java.sql.Timestamp
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasDate
import services.document.AccessibleDocumentsCount
import services.user.User

/** Visited account info **/
case class VisitedAccountInfo(
  user: User, 
  accessibleDocuments: AccessibleDocumentsCount)

object VisitedAccountInfo extends HasDate {

  implicit val accessibleDocumentsWrites: Writes[AccessibleDocumentsCount] = (
    (JsPath \ "public").write[Long] and
    (JsPath \ "shared_with_me").writeNullable[Long]
  )(d => (d.public, d.shared))  

  implicit val visitedAccountInfoWrites: Writes[VisitedAccountInfo] = (
    (JsPath \ "username").write[String] and
    (JsPath \ "real_name").writeNullable[String] and
    (JsPath \ "member_since").write[DateTime] and
    (JsPath \ "bio").writeNullable[String] and
    (JsPath \ "website").writeNullable[String] and
    (JsPath \ "documents").write[AccessibleDocumentsCount]
  )(v => (
      v.user.username,
      v.user.realName,
      new DateTime(v.user.memberSince.getTime),
      v.user.bio,
      v.user.website,
      v.accessibleDocuments
  ))  
  
}
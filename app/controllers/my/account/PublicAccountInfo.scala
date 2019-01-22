package controllers.my.account

import java.sql.Timestamp
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasDate
import services.contribution.stats.ContributorStats
import services.document.AccessibleDocumentsCount
import services.user.User

/** Visited account info **/
case class PublicAccountInfo(
  user: User, 
  accessibleDocuments: AccessibleDocumentsCount,
  stats: ContributorStats)

object PublicAccountInfo extends HasDate {

  implicit val accessibleDocumentsWrites: Writes[AccessibleDocumentsCount] = (
    (JsPath \ "public").write[Long] and
    (JsPath \ "shared_with_me").writeNullable[Long]
  )(d => (d.public, d.shared))  

  implicit val visitedAccountInfoWrites: Writes[PublicAccountInfo] = (
    (JsPath \ "username").write[String] and
    (JsPath \ "real_name").writeNullable[String] and
    (JsPath \ "member_since").write[DateTime] and
    (JsPath \ "bio").writeNullable[String] and
    (JsPath \ "website").writeNullable[String] and
    (JsPath \ "documents").write[AccessibleDocumentsCount] and
    (JsPath \ "stats").write[ContributorStats]
  )(v => (
      v.user.username,
      v.user.realName,
      new DateTime(v.user.memberSince.getTime),
      v.user.bio,
      v.user.website,
      v.accessibleDocuments,
      v.stats
  ))  
  
}